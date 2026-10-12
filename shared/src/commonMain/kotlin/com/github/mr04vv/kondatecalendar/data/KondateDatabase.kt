package com.github.mr04vv.kondatecalendar.data

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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

/** Reads everything once so it can be moved to the server. */
@Dao
interface KondateDao {
    @Query("SELECT * FROM dish")
    suspend fun dishes(): List<LegacyDish>

    @Query("SELECT * FROM meal_slot")
    suspend fun slots(): List<LegacyMealSlot>

    @Query("SELECT * FROM shopping_item ORDER BY id")
    suspend fun shoppingItems(): List<LegacyShoppingItem>
}

@Database(entities = [LegacyDish::class, LegacyMealSlot::class, LegacyShoppingItem::class], version = 3)
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
