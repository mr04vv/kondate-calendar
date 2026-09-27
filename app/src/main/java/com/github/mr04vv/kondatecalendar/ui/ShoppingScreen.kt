package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mr04vv.kondatecalendar.data.ShoppingItem

@Composable
fun ShoppingScreen(
    items: List<ShoppingItem>,
    onToggle: (ShoppingItem) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    onDeleteChecked: () -> Unit,
    onAdd: (name: String, amount: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    fun add() {
        if (name.isBlank()) return
        onAdd(name, amount)
        name = ""
        amount = ""
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("買い物リスト", style = HeadingLarge, fontSize = 26.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onDeleteChecked, enabled = items.any { it.checked }) { Text("チェック済みを消す") }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("材料名") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("分量") },
                singleLine = true,
                modifier = Modifier.width(96.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
            )
            IconButton(onClick = ::add, enabled = name.isNotBlank()) {
                Icon(Icons.Default.Add, contentDescription = "追加")
            }
        }
        if (items.isEmpty()) {
            EmptyNote(
                "買い物リストは空です。今日の画面やレシピ、カレンダーの週の見出しから材料を追加できます。",
                Modifier.padding(20.dp),
            )
        }
        LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(items, key = { it.id }) { item -> ShoppingRow(item, onToggle, onDelete) }
        }
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, onToggle: (ShoppingItem) -> Unit, onDelete: (ShoppingItem) -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDelete(item)
            value != SwipeToDismissBoxValue.Settled
        },
    )
    val colors = MaterialTheme.colorScheme
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(colors.errorContainer).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = "削除", tint = colors.onErrorContainer)
            }
        },
    ) {
        Column(Modifier.background(colors.background)) {
            Row(
                Modifier.fillMaxWidth().clickable { onToggle(item) }.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = item.checked, onCheckedChange = { onToggle(item) })
                val faded = if (item.checked) colors.onSurfaceVariant else colors.onBackground
                val decoration = if (item.checked) TextDecoration.LineThrough else null
                Text(item.name, fontSize = 16.sp, color = faded, textDecoration = decoration, modifier = Modifier.weight(1f))
                Text(
                    item.amount,
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant,
                    textDecoration = decoration,
                    modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                )
            }
            HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}
