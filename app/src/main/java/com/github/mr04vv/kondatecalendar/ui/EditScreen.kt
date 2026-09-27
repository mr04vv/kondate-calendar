package com.github.mr04vv.kondatecalendar.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.DishType
import com.github.mr04vv.kondatecalendar.data.Genre
import com.github.mr04vv.kondatecalendar.data.Ingredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

private const val DEFAULT_EMOJI = "🍽️"
private const val PHOTO_DIR = "photos"
private const val MIN_STEP_LINES = 5

// ponytail: form state lives in remember (the activity handles config changes itself); switch to
// rememberSaveable if losing a half-filled form after process death while the camera is open matters.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(original: Dish?, onSave: (Dish) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingCameraFile
        if (saved && file != null) photoPath = file.path else file?.delete()
        pendingCameraFile = null
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    photoPath = copyIntoPhotos(context, uri).path
                } catch (e: IOException) {
                    Toast.makeText(context, "写真を読み込めませんでした: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
            if (!isPreset) {
                SectionTitle("写真")
                photoPath?.let { rememberPhoto(it) }?.let { photo ->
                    Image(
                        photo,
                        contentDescription = "料理の写真",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(160.dp).clip(RoundedCornerShape(16.dp)),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val file = newPhotoFile(context)
                        pendingCameraFile = file
                        try {
                            takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                        } catch (e: ActivityNotFoundException) {
                            pendingCameraFile = null
                            Toast.makeText(context, "カメラアプリが見つかりません", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("カメラで撮る") }
                    OutlinedButton(onClick = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("ギャラリーから選ぶ") }
                }
                if (photoPath != null) {
                    TextButton(onClick = { photoPath = null }) { Text("写真を外す") }
                }
            }
            Row(Modifier.height(24.dp)) {}
        }
    }
}

private fun newPhotoFile(context: Context): File =
    File(context.filesDir, PHOTO_DIR).apply { mkdirs() }.let { File(it, "${UUID.randomUUID()}.jpg") }

// ponytail: a photo picked or taken but never saved stays in filesDir; sweep unreferenced files if storage matters.
private suspend fun copyIntoPhotos(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val file = newPhotoFile(context)
    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("cannot open $uri")
    input.use { source -> file.outputStream().use { source.copyTo(it) } }
    file
}
