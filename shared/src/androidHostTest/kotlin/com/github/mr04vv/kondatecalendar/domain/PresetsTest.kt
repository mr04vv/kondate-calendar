package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.DishType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PresetsTest {

    private val presets = parsePresets(File(PRESETS_PATH).readText())

    private val karaage = presets.first().toDish()
    private val ownDish = karaage.copy(id = "abc", presetKey = null, name = "自作の煮物")

    @Test
    fun withNothingStoredEveryPresetIsShown() {
        assertEquals(presets.map { it.toDish() }.sortedBy { it.name }, mergeDishes(presets.map { it.toDish() }, stored = emptyList()))
    }

    @Test
    fun aStoredEditReplacesItsPreset() {
        val edited = karaage.copy(canCook = true, steps = "揚げる")
        assertEquals(listOf(edited), mergeDishes(listOf(karaage), stored = listOf(edited)))
    }

    @Test
    fun ownDishesAreShownAlongsidePresetsByName() {
        assertEquals(listOf(karaage, ownDish).sortedBy { it.name }, mergeDishes(listOf(karaage), stored = listOf(ownDish)))
    }

    @Test
    fun anEditedPresetDroppedFromTheBundleIsStillShown() {
        assertEquals(listOf(karaage), mergeDishes(emptyList(), stored = listOf(karaage)))
    }

    @Test
    fun bundledPresetsCoverMostDishes() {
        assertTrue("only ${presets.size} presets", presets.size >= MIN_PRESET_COUNT)
    }

    @Test
    fun bundledPresetKeysAndNamesAreUnique() {
        assertEquals(presets.size, presets.map { it.key }.toSet().size)
        assertEquals(presets.size, presets.map { it.name }.toSet().size)
    }

    @Test
    fun bundledPresetsAreComplete() {
        presets.forEach { p ->
            assertTrue(p.key, p.key.matches(Regex("[a-z0-9-]+")))
            assertTrue(p.key, p.name.isNotBlank() && p.name.length <= MAX_NAME_LENGTH)
            assertTrue(p.key, p.emoji.isNotBlank())
            assertTrue(p.key, p.types.isNotEmpty() && p.types.size == p.types.toSet().size)
            assertTrue(p.key, !(DishType.DON in p.types && DishType.RICE in p.types))
            assertTrue(p.key, p.minutes > 0)
            assertTrue(p.key, p.ingredients.isNotEmpty() && p.ingredients.all { it.name.isNotBlank() })
            assertTrue(p.key, p.steps.isNotEmpty() && p.steps.all { it.isNotBlank() && '\n' !in it })
        }
    }

    @Test
    fun presetBecomesADishWithOneStepPerLine() {
        val p = presets.first()
        val dish = p.toDish()
        assertEquals(p.key, dish.id)
        assertEquals(p.key, dish.presetKey)
        assertEquals(p.steps, dish.steps.lines())
    }

    private companion object {
        const val PRESETS_PATH = "src/commonMain/composeResources/files/presets.json"
        const val MIN_PRESET_COUNT = 200
        const val MAX_NAME_LENGTH = 10
    }
}
