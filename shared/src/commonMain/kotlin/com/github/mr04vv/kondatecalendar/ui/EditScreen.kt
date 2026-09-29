package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.DishType
import com.github.mr04vv.kondatecalendar.data.Genre
import com.github.mr04vv.kondatecalendar.data.Ingredient

private const val DEFAULT_EMOJI = "🍽️"
private const val MIN_STEP_LINES = 5

// ponytail: form state lives in remember (the activity handles config changes itself); switch to
// rememberSaveable if losing a half-filled form after process death while the camera is open matters.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(original: Dish?, onSave: (Dish) -> Unit, onBack: () -> Unit) {
    val isPreset = original?.presetKey != null
    var name by remember { mutableStateOf(original?.name ?: "") }
    var emoji by remember { mutableStateOf(original?.emoji ?: "") }
    var genre by remember { mutableStateOf(original?.genre ?: Genre.WASHOKU) }
    var types by remember { mutableStateOf(original?.types ?: emptySet()) }
    var minutes by remember { mutableStateOf(original?.minutes?.toString() ?: "") }
    val ingredients = remember {
        mutableStateListOf<Ingredient>().apply { addAll(original?.ingredients.orEmpty().ifEmpty { listOf(Ingredient("", "")) }) }
    }
    var steps by remember { mutableStateOf(original?.steps ?: "") }
    var photoPath by remember { mutableStateOf(original?.photoPath) }
    // A dish the user adds themselves is one they can cook unless they say otherwise.
    var canCook by remember { mutableStateOf(original?.canCook ?: true) }

    fun save() {
        onSave(
            Dish(
                id = original?.id ?: 0,
                presetKey = original?.presetKey,
                name = name.trim(),
                emoji = emoji.trim().ifEmpty { DEFAULT_EMOJI },
                genre = genre,
                types = types,
                minutes = minutes.toIntOrNull(),
                ingredients = ingredients.map { Ingredient(it.name.trim(), it.amount.trim()) }.filter { it.name.isNotEmpty() },
                steps = steps.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n"),
                photoPath = if (isPreset) null else photoPath,
                canCook = canCook,
            ),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (original == null) "新しい料理" else "料理を編集") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") }
                },
                actions = { TextButton(onClick = ::save, enabled = name.isNotBlank()) { Text("保存") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("絵文字") },
                    placeholder = { Text(DEFAULT_EMOJI) },
                    singleLine = true,
                    modifier = Modifier.width(96.dp),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("料理名（必須）") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            CanCookChip(selected = canCook, onClick = { canCook = !canCook })
            SectionTitle("ジャンル")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Genre.entries.forEach { g ->
                    FilterChip(
                        selected = genre == g,
                        onClick = { genre = g },
                        label = { Text(g.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = g.color,
                            selectedContainerColor = g.color,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            SectionTitle("タイプ（複数可）")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DishType.entries.forEach { t ->
                    FilterChip(
                        selected = t in types,
                        onClick = { types = if (t in types) types - t else types + t },
                        label = { Text(t.label) },
                    )
                }
            }
            OutlinedTextField(
                value = minutes,
                onValueChange = { value -> minutes = value.filter(Char::isDigit) },
                label = { Text("所要時間（分）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(160.dp),
            )
            SectionTitle("材料")
            ingredients.forEachIndexed { i, ingredient ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ingredient.name,
                        onValueChange = { ingredients[i] = ingredient.copy(name = it) },
                        label = { Text("材料名") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = ingredient.amount,
                        onValueChange = { ingredients[i] = ingredient.copy(amount = it) },
                        label = { Text("分量") },
                        singleLine = true,
                        modifier = Modifier.width(100.dp),
                    )
                    IconButton(onClick = { ingredients.removeAt(i) }) {
                        Icon(Icons.Default.Close, contentDescription = "${ingredient.name}を削除")
                    }
                }
            }
            TextButton(onClick = { ingredients += Ingredient("", "") }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("材料を追加")
            }
            SectionTitle("作り方")
            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it },
                label = { Text("1 行に 1 手順") },
                minLines = MIN_STEP_LINES,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!isPreset) PhotoSection(photoPath, onChange = { photoPath = it })
            Row(Modifier.height(24.dp)) {}
        }
    }
}
