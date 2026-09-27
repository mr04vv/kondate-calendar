package com.github.mr04vv.kondatecalendar.feedback

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mr04vv.kondatecalendar.BuildConfig
import com.github.mr04vv.kondatecalendar.ui.EmptyNote
import com.github.mr04vv.kondatecalendar.ui.HeadingMedium
import kotlinx.coroutines.launch
import java.io.IOException

private const val MIN_DETAIL_LINES = 6

private sealed interface SendState {
    data object Editing : SendState
    data object Sending : SendState
    data class Sent(val issue: CreatedIssue) : SendState
    data class Failed(val message: String) : SendState
}

private fun environment(): String =
    "アプリ: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
        "端末: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf(FeedbackKind.FEATURE) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<SendState>(SendState.Editing) }
    val draft = FeedbackDraft(kind, title, detail)

    fun send() {
        state = SendState.Sending
        scope.launch {
            state = try {
                SendState.Sent(GitHubIssues.create(toNewIssue(draft, environment())))
            } catch (e: IOException) {
                SendState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("要望・不具合を送る") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val sent = state as? SendState.Sent
            if (sent != null) {
                Text("#${sent.issue.number} として送信しました", style = HeadingMedium)
                EmptyNote(sent.issue.url)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sent.issue.url)))
                    }) { Text("GitHub で開く") }
                    Button(onClick = {
                        title = ""
                        detail = ""
                        state = SendState.Editing
                    }) { Text("続けて送る") }
                }
                return@Column
            }
            if (!GitHubIssues.isConfigured) {
                Text(
                    "送信用のトークンが設定されていません。local.properties に github.issueToken を書いてビルドし直してください。",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
            }
            EmptyNote("${BuildConfig.GITHUB_REPO} に Issue として登録されます。リポジトリは公開なので、内容は誰でも読めます。")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                FeedbackKind.entries.forEachIndexed { i, k ->
                    SegmentedButton(
                        selected = kind == k,
                        onClick = { kind = k },
                        shape = SegmentedButtonDefaults.itemShape(i, FeedbackKind.entries.size),
                    ) { Text(k.label) }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("タイトル（必須）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it },
                label = { Text(if (kind == FeedbackKind.BUG) "起きたこと・手順" else "ほしい機能・理由") },
                minLines = MIN_DETAIL_LINES,
                modifier = Modifier.fillMaxWidth(),
            )
            EmptyNote("アプリのバージョンと端末の機種名・Android のバージョンが本文の末尾に付きます。")
            (state as? SendState.Failed)?.let {
                Text("送信できませんでした: ${it.message}", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
            Button(
                onClick = ::send,
                enabled = draft.isSendable && GitHubIssues.isConfigured && state != SendState.Sending,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state == SendState.Sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("送信")
                }
            }
        }
    }
}
