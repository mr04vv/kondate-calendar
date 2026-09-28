package com.github.mr04vv.kondatecalendar.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mr04vv.kondatecalendar.KondateApp
import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.Meal
import com.github.mr04vv.kondatecalendar.data.MealSlot
import com.github.mr04vv.kondatecalendar.data.ShoppingItem
import com.github.mr04vv.kondatecalendar.domain.ShoppingGroup
import com.github.mr04vv.kondatecalendar.domain.ShoppingSource
import com.github.mr04vv.kondatecalendar.domain.parsePresets
import com.github.mr04vv.kondatecalendar.domain.presetsToInsert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class SlotKey(val date: LocalDate, val meal: Meal)

/** Every slot planned on [dates], in date and meal order. */
fun Map<SlotKey, Dish>.sourcesOn(dates: List<LocalDate>): List<ShoppingSource.Slot> =
    dates.flatMap { date ->
        Meal.entries.mapNotNull { meal -> this[SlotKey(date, meal)]?.let { ShoppingSource.Slot(date, meal, it) } }
    }

enum class Tab { CALENDAR, TODAY, SHOPPING }

sealed interface Screen {
    data class Recipe(val dishId: Long) : Screen

    /** [dishId] null creates a new dish; [assignTo] puts the saved dish into that slot. */
    data class Edit(val dishId: Long?, val assignTo: SlotKey? = null) : Screen

    data object Feedback : Screen
}

class KondateViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as KondateApp).db.dao()

    val dishes: StateFlow<List<Dish>> = dao.dishes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ponytail: every slot ever planned is kept in memory (about 1,100 per year); query by date range if that grows.
    val plan: StateFlow<Map<SlotKey, Dish>> = combine(dao.slots(), dishes) { slots, dishes ->
        val byId = dishes.associateBy { it.id }
        slots.mapNotNull { slot -> byId[slot.dishId]?.let { SlotKey(slot.date, slot.meal) to it } }.toMap()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val shopping: StateFlow<List<ShoppingItem>> =
        dao.shoppingItems().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var tab by mutableStateOf(Tab.CALENDAR)
    val stack = mutableStateListOf<Screen>()

    /** The slot whose picker sheet is open. */
    var picking by mutableStateOf<SlotKey?>(null)

    /** Whether the picker starts limited to cookable dishes; kept for the session. */
    var pickerCanCookOnly by mutableStateOf(false)

    /** The filled slot whose action menu is open. */
    var slotMenu by mutableStateOf<SlotKey?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) { seedPresets() }
    }

    private suspend fun seedPresets() {
        val json = getApplication<Application>().assets.open(PRESETS_ASSET).bufferedReader().use { it.readText() }
        val missing = presetsToInsert(parsePresets(json), dao.presetKeys().toSet())
        dao.insertDishes(missing.map { it.toDish() })
    }

    fun dishById(id: Long): Dish? = dishes.value.firstOrNull { it.id == id }

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

    fun assign(key: SlotKey, dishId: Long) = viewModelScope.launch {
        dao.assignSlot(MealSlot(key.date, key.meal, dishId))
        picking = null
    }

    fun clear(key: SlotKey) = viewModelScope.launch {
        dao.clearSlot(key.date, key.meal)
        slotMenu = null
    }

    fun toggleCanCook(dish: Dish) = viewModelScope.launch { dao.updateDish(dish.copy(canCook = !dish.canCook)) }

    /** [onAdded] receives how many rows went in; zero means every source was already in the list. */
    fun addToShopping(sources: List<ShoppingSource>, onAdded: (Int) -> Unit) = viewModelScope.launch {
        onAdded(dao.addToShoppingList(sources))
    }

    fun addManualItem(name: String, amount: String) = viewModelScope.launch {
        dao.insertShoppingItems(listOf(ShoppingItem(name = name.trim(), amount = amount.trim())))
    }

    fun toggle(group: ShoppingGroup) = viewModelScope.launch {
        dao.updateShoppingItems(group.items.map { it.copy(checked = !group.checked) })
    }

    fun delete(group: ShoppingGroup) = viewModelScope.launch { dao.deleteShoppingItems(group.items) }

    fun deleteChecked() = viewModelScope.launch { dao.deleteCheckedShoppingItems() }

    fun save(dish: Dish, assignTo: SlotKey?) = viewModelScope.launch {
        val previousPhoto = dishById(dish.id)?.photoPath
        val id = if (dish.id == 0L) dao.insertDish(dish) else dish.id.also { dao.updateDish(dish) }
        if (previousPhoto != null && previousPhoto != dish.photoPath) File(previousPhoto).delete()
        if (assignTo != null) dao.assignSlot(MealSlot(assignTo.date, assignTo.meal, id))
        back()
    }

    private companion object {
        const val PRESETS_ASSET = "presets.json"
    }
}
