package com.github.mr04vv.kondatecalendar.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

// The local database's tables, kept only so data from before the move to the server can be read once.
// ponytail: delete with KondateDatabase once every device has moved its data.

@Entity(tableName = "dish", indices = [Index(value = ["presetKey"], unique = true)])
data class LegacyDish(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetKey: String? = null,
    val name: String,
    val emoji: String,
    val genre: Genre,
    val types: Set<DishType>,
    val minutes: Int? = null,
    val ingredients: List<Ingredient>,
    val steps: String,
    val photoPath: String? = null,
    @ColumnInfo(defaultValue = "0") val canCook: Boolean = false,
)

@Entity(
    tableName = "meal_slot",
    primaryKeys = ["date", "meal"],
    foreignKeys = [
        ForeignKey(
            entity = LegacyDish::class,
            parentColumns = ["id"],
            childColumns = ["dishId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dishId")],
)
data class LegacyMealSlot(
    val date: LocalDate,
    val meal: Meal,
    val dishId: Long,
)

@Entity(tableName = "shopping_item")
data class LegacyShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: String,
    val checked: Boolean = false,
    val date: LocalDate? = null,
    val meal: Meal? = null,
    val dishId: Long? = null,
    val dishName: String? = null,
)
