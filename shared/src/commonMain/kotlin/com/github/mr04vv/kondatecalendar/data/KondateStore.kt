package com.github.mr04vv.kondatecalendar.data

import com.github.mr04vv.kondatecalendar.domain.MigrationData
import com.github.mr04vv.kondatecalendar.domain.slotMove
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.WriteBatch
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

private typealias BatchOp = WriteBatch.() -> Unit

/** Whether the user's server data is safe to replace with this device's local data. */
enum class ServerState { EMPTY, HAS_DATA }

/**
 * The user's data on Firestore under `users/{uid}`. Reads follow the local cache, so other devices' edits arrive on
 * their own and the app keeps working offline. Everyday writes are not awaited: Firestore applies them to the cache
 * at once and sends them when it can, and failures arrive in [errors].
 */
class KondateStore(uid: String, private val scope: CoroutineScope) {
    private val db = Firebase.firestore
    private val user = db.collection(USERS).document(uid)
    private val dishes = user.collection(DISHES)
    private val slots = user.collection(SLOTS)
    private val shopping = user.collection(SHOPPING)
    private val migration = user.collection(META).document(MIGRATION)

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val errors: SharedFlow<String> = _errors

    /** The dishes the user created or edited; bundled presets are not stored until edited. */
    fun dishes(): Flow<List<Dish>> = dishes.watch { it.data<Dish>() }

    fun slots(): Flow<List<MealSlot>> = slots.watch { it.data<MealSlot>() }

    fun shoppingItems(): Flow<List<ShoppingItem>> = shopping.watch { it.data<ShoppingItem>() }

    /** Saves [dish], giving a new one an id; returns the id. */
    fun saveDish(dish: Dish): String {
        val saved = if (dish.id.isEmpty()) dish.copy(id = dishes.document.id) else dish
        write(listOf(setOp(dishes.document(saved.id), saved)))
        return saved.id
    }

    /** Puts a dish in a slot; the previous dish's unchecked ingredients leave the shopping list. */
    fun assignSlot(slot: MealSlot, current: List<ShoppingItem>) {
        val taken = current.filter { it.isOpenFrom(slot.date, slot.meal) && it.dishId != slot.dishId }
        write(taken.map { deleteOp(shopping.document(it.id)) } + setOp(slots.document(slotId(slot.date, slot.meal)), slot))
    }

    fun clearSlot(date: LocalDate, meal: Meal, current: List<ShoppingItem>) {
        val taken = current.filter { it.isOpenFrom(date, meal) }
        write(taken.map { deleteOp(shopping.document(it.id)) } + deleteOp(slots.document(slotId(date, meal))))
    }

    /** Drops one planned slot onto another, swapping their dishes; see [slotMove]. */
    fun moveSlot(from: MealSlot, toDate: LocalDate, toMeal: Meal, toDishId: String?, current: List<ShoppingItem>) {
        val move = slotMove(from, toDate, toMeal, toDishId, current)
        write(
            listOfNotNull(move.vacated?.let { deleteOp(slots.document(slotId(it.date, it.meal))) }) +
                move.slots.map { setOp(slots.document(slotId(it.date, it.meal)), it) } +
                move.items.map { setOp(shopping.document(it.id), it) },
        )
    }

    /** Adds [items] in order after every row already in the list. */
    fun insertShoppingItems(items: List<ShoppingItem>) = write(insertOps(items))

    /** Empties the list ([current] is everything in it) and fills it with [items], in one go. */
    fun replaceShoppingItems(current: List<ShoppingItem>, items: List<ShoppingItem>) =
        write(current.map { deleteOp(shopping.document(it.id)) } + insertOps(items))

    fun updateShoppingItems(items: List<ShoppingItem>) = write(items.map { setOp(shopping.document(it.id), it) })

    fun deleteShoppingItems(items: List<ShoppingItem>) = write(items.map { deleteOp(shopping.document(it.id)) })

    /**
     * Reads the server, never the cache, so an offline device cannot mistake an empty cache for an empty server; throws
     * when the server cannot be reached. A move that never finished counts as empty, so it is redone from scratch.
     */
    suspend fun serverState(): ServerState = withTimeout(SERVER_TIMEOUT) {
        val marker = migration.get(Source.SERVER)
        when {
            marker.exists -> if (marker.data<MigrationMarker>().done) ServerState.HAS_DATA else ServerState.EMPTY
            listOf(dishes, slots, shopping).any { it.limit(1).get(Source.SERVER).documents.isNotEmpty() } -> ServerState.HAS_DATA
            else -> ServerState.EMPTY
        }
    }

    /**
     * Replaces everything on the server with [data] and returns once the server holds all of it. Marks the move as
     * unfinished first, so an interruption anywhere leaves the server counted as empty and the move can be redone.
     * Throws when the server cannot be reached or does not end up with every document.
     */
    suspend fun replaceAll(data: MigrationData) = withTimeout(SERVER_TIMEOUT) {
        migration.set(MigrationMarker(done = false))
        val stale = listOf(dishes, slots, shopping).flatMap { it.get(Source.SERVER).documents.map { doc -> doc.reference } }
        commitAll(stale.map(::deleteOp))
        commitAll(
            data.dishes.map { setOp(dishes.document(it.id), it) } +
                data.slots.map { setOp(slots.document(slotId(it.date, it.meal)), it) } +
                data.shopping.map { setOp(shopping.document(it.id), it) },
        )
        val expected = listOf(dishes to data.dishes.size, slots to data.slots.size, shopping to data.shopping.size)
        expected.forEach { (collection, count) ->
            val found = collection.get(Source.SERVER).documents.size
            check(found == count) { "${collection.path} has $found documents on the server, expected $count" }
        }
        migration.set(MigrationMarker(done = true))
    }

    private fun <T> CollectionReference.watch(decode: (DocumentSnapshot) -> T): Flow<List<T>> =
        snapshots.map { snapshot -> snapshot.documents.map(decode) }
            .catch { e -> _errors.tryEmit("データを読み込めませんでした: ${e.message}") }

    private inline fun <reified T : Any> setOp(ref: DocumentReference, value: T): BatchOp = { set(ref, value) }

    private fun deleteOp(ref: DocumentReference): BatchOp = { delete(ref) }

    private fun insertOps(items: List<ShoppingItem>): List<BatchOp> {
        val now = Clock.System.now().toEpochMilliseconds()
        return items.mapIndexed { index, item ->
            val ref = shopping.document
            setOp(ref, item.copy(id = ref.id, createdAt = now + index))
        }
    }

    /** Applies [ops] in batches without waiting for the server; see the class comment. */
    private fun write(ops: List<BatchOp>) {
        if (ops.isEmpty()) return
        // The batch is built and handed to Firestore here, so the cache updates before this returns.
        val batches = ops.chunked(MAX_BATCH_OPS).map { chunk -> db.batch().also { batch -> chunk.forEach { batch.it() } } }
        scope.launch {
            try {
                batches.forEach { it.commit() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errors.tryEmit("保存できませんでした: ${e.message}")
            }
        }
    }

    /** Applies [ops] in batches and returns once the server has accepted every one. */
    private suspend fun commitAll(ops: List<BatchOp>) {
        ops.chunked(MAX_BATCH_OPS).forEach { chunk -> db.batch().also { batch -> chunk.forEach { batch.it() } }.commit() }
    }

    private fun ShoppingItem.isOpenFrom(date: LocalDate, meal: Meal) = !checked && this.date == date && this.meal == meal

    private fun slotId(date: LocalDate, meal: Meal) = "${date}_$meal"

    @Serializable
    private data class MigrationMarker(val done: Boolean)

    private companion object {
        const val USERS = "users"
        const val DISHES = "dishes"
        const val SLOTS = "slots"
        const val SHOPPING = "shopping"
        const val META = "meta"
        const val MIGRATION = "migration"

        /** Firestore's limit on writes in one batch. */
        const val MAX_BATCH_OPS = 500

        /** Server reads and acknowledged writes wait forever offline; give up so the move can report it. */
        val SERVER_TIMEOUT = 2.minutes
    }
}
