package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mr04vv.kondatecalendar.auth.GoogleSignIn
import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.KondateStore
import com.github.mr04vv.kondatecalendar.data.LegacyDatabase
import com.github.mr04vv.kondatecalendar.data.Meal
import com.github.mr04vv.kondatecalendar.data.MealSlot
import com.github.mr04vv.kondatecalendar.data.ServerState
import com.github.mr04vv.kondatecalendar.data.ShoppingItem
import com.github.mr04vv.kondatecalendar.domain.MigrationData
import com.github.mr04vv.kondatecalendar.domain.ShoppingGroup
import com.github.mr04vv.kondatecalendar.domain.ShoppingSource
import com.github.mr04vv.kondatecalendar.domain.expiredShoppingRows
import com.github.mr04vv.kondatecalendar.domain.mergeDishes
import com.github.mr04vv.kondatecalendar.domain.parsePresets
import com.github.mr04vv.kondatecalendar.domain.shoppingRowsToAdd
import com.github.mr04vv.kondatecalendar.resources.Res
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

data class SlotKey(val date: LocalDate, val meal: Meal)

/** Every slot planned on [dates], in date and meal order. */
fun Map<SlotKey, Dish>.sourcesOn(dates: List<LocalDate>): List<ShoppingSource.Slot> =
    dates.flatMap { date ->
        Meal.entries.mapNotNull { meal -> this[SlotKey(date, meal)]?.let { ShoppingSource.Slot(date, meal, it) } }
    }

enum class Tab { CALENDAR, TODAY, SHOPPING }

/** Moving the local database to the server after sign-in; see [KondateViewModel.migration]. */
sealed interface Migration {
    data object Checking : Migration

    /** The server holds nothing yet, so this device's [data] can go there. */
    data class Offer(val data: MigrationData) : Migration

    /** The server already holds data, which wins; this device's local data is set aside. */
    data object Discard : Migration

    data object Moving : Migration

    data class Failed(val message: String) : Migration
}

sealed interface Screen {
    data class Recipe(val dishId: String) : Screen

    /** [dishId] null creates a new dish; [assignTo] puts the saved dish into that slot. */
    data class Edit(val dishId: String?, val assignTo: SlotKey? = null) : Screen

    data object Settings : Screen

    data object Feedback : Screen
}

@OptIn(ExperimentalCoroutinesApi::class)
class KondateViewModel(private val legacy: LegacyDatabase) : ViewModel() {
    /** False when the build has no Firebase config file; the app then only says so. */
    val configured: Boolean = runCatching { Firebase.app }.isSuccess

    val user: StateFlow<FirebaseUser?> =
        if (configured) {
            Firebase.auth.authStateChanged.stateIn(viewModelScope, SharingStarted.Eagerly, Firebase.auth.currentUser)
        } else {
            MutableStateFlow(null)
        }

    private val store: StateFlow<KondateStore?> = user.distinctUntilChangedBy { it?.uid }
        .map { it?.let { signedIn -> KondateStore(signedIn.uid, viewModelScope) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val presets = MutableStateFlow<List<Dish>>(emptyList())

    val dishes: StateFlow<List<Dish>> = combine(presets, store.flatMapLatest { it?.dishes() ?: flowOf(emptyList()) }, ::mergeDishes)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ponytail: every slot ever planned is kept in memory (about 1,100 per year); query by date range if that grows.
    val plan: StateFlow<Map<SlotKey, Dish>> = combine(store.flatMapLatest { it?.slots() ?: flowOf(emptyList()) }, dishes) { slots, dishes ->
        val byId = dishes.associateBy { it.id }
        slots.mapNotNull { slot -> byId[slot.dishId]?.let { SlotKey(slot.date, slot.meal) to it } }.toMap()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val shopping: StateFlow<List<ShoppingItem>> = store.flatMapLatest { it?.shoppingItems() ?: flowOf(emptyList()) }
        .map { items -> items.sortedWith(compareBy<ShoppingItem>({ it.checked }, { it.createdAt }, { it.id })) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Errors to show briefly: failed sign-in, reads and writes. */
    val messages: SharedFlow<String> = _messages

    /** Non-null while this device's local database is being moved to the server; a dialog covers the app meanwhile. */
    var migration by mutableStateOf<Migration?>(null)
        private set

    var tab by mutableStateOf(Tab.CALENDAR)
    val stack = mutableStateListOf<Screen>()

    /** The slot whose picker sheet is open. */
    var picking by mutableStateOf<SlotKey?>(null)

    /** Whether the picker starts limited to cookable dishes; kept for the session. */
    var pickerCanCookOnly by mutableStateOf(false)

    /** The filled slot whose action menu is open. */
    var slotMenu by mutableStateOf<SlotKey?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            presets.value = parsePresets(Res.readBytes(PRESETS_FILE).decodeToString()).map { it.toDish() }
        }
        viewModelScope.launch { store.flatMapLatest { it?.errors ?: emptyFlow() }.collect { _messages.emit(it) } }
        viewModelScope.launch { store.filterNotNull().collect { checkMigration() } }
    }

    fun signIn(google: GoogleSignIn) = viewModelScope.launch {
        try {
            val tokens = google() ?: return@launch
            Firebase.auth.signInWithCredential(GoogleAuthProvider.credential(tokens.idToken, tokens.accessToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _messages.emit("ログインできませんでした: ${e.message}")
        }
    }

    fun signOut() = viewModelScope.launch {
        stack.clear()
        picking = null
        slotMenu = null
        migration = null
        Firebase.auth.signOut()
    }

    /** Looks for a local database left from before the move to the server and decides what to offer. */
    fun checkMigration() = runMigrationStep {
        if (!legacy.exists()) return@runMigrationStep null
        migration = Migration.Checking
        val data = withContext(Dispatchers.IO) { legacy.read(presets.first { it.isNotEmpty() }) }
        if (data.isEmpty()) {
            legacy.setAside()
            return@runMigrationStep null
        }
        when (currentStore().serverState()) {
            ServerState.EMPTY -> Migration.Offer(data)
            ServerState.HAS_DATA -> Migration.Discard
        }
    }

    /** Sends [data] to the server; the local file is set aside only after the server holds all of it. */
    fun moveToServer(data: MigrationData) = runMigrationStep {
        migration = Migration.Moving
        currentStore().replaceAll(data)
        legacy.setAside()
        null
    }

    /** Starts with an empty server, clearing anything an interrupted move left there. */
    fun startWithoutLocalData() = moveToServer(MigrationData(emptyList(), emptyList(), emptyList()))

    fun discardLocalData() = runMigrationStep {
        legacy.setAside()
        null
    }

    /** Runs a step and shows its result; any failure leaves the local file untouched and offers a retry. */
    private fun runMigrationStep(step: suspend () -> Migration?) = viewModelScope.launch {
        migration = try {
            step()
        } catch (e: TimeoutCancellationException) {
            Migration.Failed("サーバーから応答がありません。通信できる場所でやり直してください。")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Migration.Failed("${e.message}")
        }
    }

    private fun currentStore(): KondateStore = checkNotNull(store.value) { "ログインしていません" }

    fun dishById(id: String): Dish? = dishes.value.firstOrNull { it.id == id }

    fun open(screen: Screen) {
        picking = null
        slotMenu = null
        stack += screen
    }

    fun back() {
        stack.removeLastOrNull()
    }

    /** An empty slot opens the picker; a filled one opens its menu. */
    fun tapSlot(key: SlotKey) {
        if (key in plan.value) slotMenu = key else picking = key
    }

    fun assign(key: SlotKey, dishId: String) {
        store.value?.assignSlot(MealSlot(key.date, key.meal, dishId), shopping.value)
        picking = null
    }

    fun clear(key: SlotKey) {
        store.value?.clearSlot(key.date, key.meal, shopping.value)
        slotMenu = null
    }

    fun moveSlot(from: SlotKey, to: SlotKey) {
        val dish = plan.value[from] ?: return
        store.value?.moveSlot(MealSlot(from.date, from.meal, dish.id), to.date, to.meal, plan.value[to]?.id, shopping.value)
    }

    fun toggleCanCook(dish: Dish) {
        store.value?.saveDish(dish.copy(canCook = !dish.canCook))
    }

    /** [onAdded] receives how many rows went in; zero means every source was already in the list. */
    fun addToShopping(sources: List<ShoppingSource>, onAdded: (Int) -> Unit) {
        val rows = shoppingRowsToAdd(shopping.value, sources, today())
        store.value?.insertShoppingItems(rows)
        onAdded(rows.size)
    }

    /** Replaces the whole list, manual and checked rows too, with [sources]' ingredients; [onReplaced] gets the count. */
    fun replaceShopping(sources: List<ShoppingSource>, onReplaced: (Int) -> Unit) {
        val rows = shoppingRowsToAdd(existing = emptyList(), sources, today())
        store.value?.replaceShoppingItems(shopping.value, rows)
        onReplaced(rows.size)
    }

    fun deleteExpiredShopping() {
        store.value?.deleteShoppingItems(expiredShoppingRows(shopping.value, today()))
    }

    fun addManualItem(name: String, amount: String) {
        store.value?.insertShoppingItems(listOf(ShoppingItem(name = name.trim(), amount = amount.trim())))
    }

    fun toggle(group: ShoppingGroup) {
        store.value?.updateShoppingItems(group.items.map { it.copy(checked = !group.checked) })
    }

    fun delete(group: ShoppingGroup) {
        store.value?.deleteShoppingItems(group.items)
    }

    fun deleteChecked() {
        store.value?.deleteShoppingItems(shopping.value.filter { it.checked })
    }

    fun save(dish: Dish, assignTo: SlotKey?) {
        val store = store.value ?: return
        val id = store.saveDish(dish)
        if (assignTo != null) store.assignSlot(MealSlot(assignTo.date, assignTo.meal, id), shopping.value)
        back()
    }

    private companion object {
        const val PRESETS_FILE = "files/presets.json"
    }
}
