package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/** Shown instead of the app when the build has no Firebase config file. */
@Composable
fun NotConfiguredScreen() {
    CenteredColumn {
        Text("献立カレンダー", style = HeadingMedium)
        Text(
            "Firebase の設定ファイルがないため使えません。CLAUDE.md の手順で設定ファイルを置いてからビルドしてください。",
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LoginScreen(onSignIn: () -> Unit) {
    CenteredColumn {
        Text("献立カレンダー", style = HeadingLarge)
        Text("献立は Google アカウントに保存され、同じアカウントでログインした端末どうしで同期されます。", textAlign = TextAlign.Center)
        Button(onClick = onSignIn) { Text("Google でログイン") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(email: String?, onFeedback: () -> Unit, onSignOut: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("アカウント", style = Heading)
            Text("${email ?: "Google アカウント"} でログイン中")
            OutlinedButton(onClick = onSignOut) { Text("ログアウト") }
            Text("要望・不具合", style = Heading, modifier = Modifier.padding(top = 12.dp))
            OutlinedButton(onClick = onFeedback) { Text("要望・不具合を送る") }
        }
    }
}

/** Covers the app until this device's local data has been moved to the server or set aside. */
@Composable
fun MigrationDialog(
    state: Migration,
    onMove: (Migration.Offer) -> Unit,
    onStartEmpty: () -> Unit,
    onDiscard: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    val signOut = @Composable { TextButton(onClick = onSignOut) { Text("ログアウト") } }
    when (state) {
        Migration.Checking, Migration.Moving -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(if (state == Migration.Moving) "サーバーへ移しています" else "確認しています") },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
            confirmButton = {},
        )
        is Migration.Offer -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("この端末のデータを移しますか") },
            text = {
                Text(
                    "このアカウントにはまだデータがありません。この端末の自作・編集した料理 ${state.data.dishes.size} 品、" +
                        "献立 ${state.data.slots.size} 枠、買い物リスト ${state.data.shopping.size} 行をサーバーへ移します。" +
                        "移さずに始めると、この端末のデータは使われなくなります。",
                )
            },
            confirmButton = { TextButton(onClick = { onMove(state) }) { Text("移す") } },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onStartEmpty) { Text("移さずに始める") }
                    signOut()
                }
            },
        )
        Migration.Discard -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("サーバーのデータを使います") },
            text = { Text("このアカウントにはすでにデータがあるため、この端末に残っている以前のデータは使われなくなります。") },
            confirmButton = { TextButton(onClick = onDiscard) { Text("OK") } },
            dismissButton = signOut,
        )
        is Migration.Failed -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("データを移せませんでした") },
            text = { Text("この端末のデータはそのまま残っています。\n${state.message}") },
            confirmButton = { TextButton(onClick = onRetry) { Text("やり直す") } },
            dismissButton = signOut,
        )
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
