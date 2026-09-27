package com.github.mr04vv.kondatecalendar.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class Genre(val label: String) {
    WASHOKU("和食"),
    CHUKA("中華"),
    YOSHOKU("洋食"),
    KANKOKU("韓国"),
    ETHNIC("エスニック"),
    OTHER("その他"),
}

enum class DishType(val label: String) {
    DON("丼"),
    NOODLE("麺"),
    MEAT("肉"),
    FISH("魚"),
    EGG("卵"),
    VEGETABLE("野菜"),
    SOUP("汁物"),
    BREAD("パン"),
    RICE("ご飯もの"),
}

enum class Meal(val label: String) {
    BREAKFAST("朝"),
    LUNCH("昼"),
    DINNER("夜"),
}

@Serializable
data class Ingredient(val name: String, val amount: String)

/** A dish. [presetKey] is non-null for bundled presets and null for user-created dishes. */
@Entity(tableName = "dish", indices = [Index(value = ["presetKey"], unique = true)])
data class Dish(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetKey: String? = null,
    val name: String,
    val emoji: String,
    val genre: Genre,
    val types: Set<DishType>,
    val minutes: Int? = null,
    val ingredients: List<Ingredient>,
    /** One step per line. */
    val steps: String,
    val photoPath: String? = null,
    /** The user marked this as a dish they can cook. */
    @ColumnInfo(defaultValue = "0") val canCook: Boolean = false,
)

@Entity(
    tableName = "meal_slot",
    primaryKeys = ["date", "meal"],
    foreignKeys = [
        ForeignKey(
            entity = Dish::class,
            parentColumns = ["id"],
            childColumns = ["dishId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dishId")],
)
data class MealSlot(
    val date: LocalDate,
    val meal: Meal,
    val dishId: Long,
)

@Entity(tableName = "shopping_item")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: String,
    val checked: Boolean = false,
    val manual: Boolean = false,
)
