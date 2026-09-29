package com.github.mr04vv.kondatecalendar.data

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.github.mr04vv.kondatecalendar.domain.ShoppingSource
import com.github.mr04vv.kondatecalendar.domain.shoppingRowsToAdd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class Converters {
    // Room's KSP backend null-checks around these, so they also serve the nullable ShoppingItem.date.
    @TypeConverter fun fromDate(date: LocalDate): Long = date.toEpochDays()
    @TypeConverter fun toDate(epochDay: Long): LocalDate = LocalDate.fromEpochDays(epochDay)

    @TypeConverter fun fromTypes(types: Set<DishType>): String = types.joinToString(TYPE_SEPARATOR) { it.name }
    @TypeConverter fun toTypes(value: String): Set<DishType> =
        value.split(TYPE_SEPARATOR).filter { it.isNotEmpty() }.mapTo(LinkedHashSet()) { DishType.valueOf(it) }

    @TypeConverter fun fromIngredients(ingredients: List<Ingredient>): String = Json.encodeToString(ingredients)
    @TypeConverter fun toIngredients(value: String): List<Ingredient> = Json.decodeFromString(value)

    private companion object {
        const val TYPE_SEPARATOR = ","
    }
}

@Dao
interface KondateDao {
    @Query("SELECT * FROM dish ORDER BY name")
    fun dishes(): Flow<List<Dish>>

    @Query("SELECT presetKey FROM dish WHERE presetKey IS NOT NULL")
    suspend fun presetKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDishes(dishes: List<Dish>)

    @Insert
    suspend fun insertDish(dish: Dish): Long

    @Update
    suspend fun updateDish(dish: Dish)

    @Query("SELECT * FROM meal_slot")
    fun slots(): Flow<List<MealSlot>>

    @Upsert
    suspend fun upsertSlot(slot: MealSlot)

    @Query("DELETE FROM meal_slot WHERE date = :date AND meal = :meal")
    suspend fun deleteSlot(date: LocalDate, meal: Meal)

    /** Takes back the unchecked rows a slot added for any dish other than [keepDishId]; checked rows stay. */
    @Query("DELETE FROM shopping_item WHERE date = :date AND meal = :meal AND checked = 0 AND dishId IS NOT :keepDishId")
    suspend fun deleteOpenShoppingItemsFor(date: LocalDate, meal: Meal, keepDishId: Long?)

    /** Puts a dish in a slot; the previous dish's unchecked ingredients leave the shopping list. */
    @Transaction
    suspend fun assignSlot(slot: MealSlot) {
        deleteOpenShoppingItemsFor(slot.date, slot.meal, keepDishId = slot.dishId)
        upsertSlot(slot)
    }

    @Transaction
    suspend fun clearSlot(date: LocalDate, meal: Meal) {
        deleteOpenShoppingItemsFor(date, meal, keepDishId = null)
        deleteSlot(date, meal)
    }

    @Query("SELECT * FROM shopping_item ORDER BY checked, id")
    fun shoppingItems(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_item")
    suspend fun currentShoppingItems(): List<ShoppingItem>

    @Insert
    suspend fun insertShoppingItems(items: List<ShoppingItem>)

    @Update
    suspend fun updateShoppingItems(items: List<ShoppingItem>)

    @Delete
    suspend fun deleteShoppingItems(items: List<ShoppingItem>)

    @Query("DELETE FROM shopping_item WHERE checked = 1")
    suspend fun deleteCheckedShoppingItems()

    /** Returns how many rows were added; sources already in the list add none. */
    @Transaction
    suspend fun addToShoppingList(sources: List<ShoppingSource>): Int {
        val rows = shoppingRowsToAdd(currentShoppingItems(), sources)
        insertShoppingItems(rows)
        return rows.size
    }
}

@Database(entities = [Dish::class, MealSlot::class, ShoppingItem::class], version = 3)
@TypeConverters(Converters::class)
@ConstructedBy(KondateDatabaseConstructor::class)
abstract class KondateDatabase : RoomDatabase() {
    abstract fun dao(): KondateDao

    companion object {
        const val NAME = "kondate.db"

        /** Adds the "can cook" mark; dishes the user created count as ones they can cook. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE dish ADD COLUMN canCook INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("UPDATE dish SET canCook = 1 WHERE presetKey IS NULL")
            }
        }

        /**
         * Shopping rows now record the slot or recipe they came from. Old rows carry no source and many were
         * doubled by repeated adds, so the requester chose to start the list empty; dishes and slots are kept.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE shopping_item")
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `amount` TEXT NOT NULL, `checked` INTEGER NOT NULL, `date` INTEGER, " +
                        "`meal` TEXT, `dishId` INTEGER, `dishName` TEXT)",
                )
            }
        }

        /** Finishes a platform builder, which decides where the [NAME] file lives. */
        fun build(builder: Builder<KondateDatabase>): KondateDatabase =
            builder
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}

// Room generates the actual objects.
@Suppress("KotlinNoActualForExpect")
expect object KondateDatabaseConstructor : RoomDatabaseConstructor<KondateDatabase> {
    override fun initialize(): KondateDatabase
}
