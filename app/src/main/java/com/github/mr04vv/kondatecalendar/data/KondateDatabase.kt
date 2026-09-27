package com.github.mr04vv.kondatecalendar.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.mr04vv.kondatecalendar.domain.mergeIntoShoppingList
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromDate(date: LocalDate): Long = date.toEpochDay()
    @TypeConverter fun toDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

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
    suspend fun clearSlot(date: LocalDate, meal: Meal)

    @Query("SELECT * FROM shopping_item ORDER BY checked, id")
    fun shoppingItems(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_item")
    suspend fun currentShoppingItems(): List<ShoppingItem>

    @Upsert
    suspend fun upsertShoppingItems(items: List<ShoppingItem>)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_item WHERE checked = 1")
    suspend fun deleteCheckedShoppingItems()

    @Transaction
    suspend fun addToShoppingList(ingredients: List<Ingredient>) {
        upsertShoppingItems(mergeIntoShoppingList(currentShoppingItems(), ingredients))
    }
}

@Database(entities = [Dish::class, MealSlot::class, ShoppingItem::class], version = 2)
@TypeConverters(Converters::class)
abstract class KondateDatabase : RoomDatabase() {
    abstract fun dao(): KondateDao

    companion object {
        private const val NAME = "kondate.db"

        /** Adds the "can cook" mark; dishes the user created count as ones they can cook. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE dish ADD COLUMN canCook INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE dish SET canCook = 1 WHERE presetKey IS NULL")
            }
        }

        fun create(context: Context): KondateDatabase =
            Room.databaseBuilder(context, KondateDatabase::class.java, NAME).addMigrations(MIGRATION_1_2).build()
    }
}
