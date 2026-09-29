package com.github.mr04vv.kondatecalendar.feedback

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class FeedbackKind(val label: String, val githubLabel: String) {
    FEATURE("機能要望", "enhancement"),
    BUG("不具合", "bug"),
}

data class FeedbackDraft(val kind: FeedbackKind, val title: String, val detail: String) {
    val isSendable: Boolean get() = title.isNotBlank()
}

@Serializable
data class NewIssue(val title: String, val body: String, val labels: List<String>)

data class CreatedIssue(val number: Int, val url: String)

private const val FOOTER_SEPARATOR = "---"
private const val SOURCE_NOTE = "送信元: アプリ内フォーム"

private val lenientJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class CreatedIssueResponse(val number: Int, @kotlinx.serialization.SerialName("html_url") val htmlUrl: String)

/** The GitHub issue for [draft]; [environment] (app and device) is appended so reports can be reproduced. */
fun toNewIssue(draft: FeedbackDraft, environment: String): NewIssue {
    val footer = "$FOOTER_SEPARATOR\n$environment\n$SOURCE_NOTE"
    val detail = draft.detail.trim()
    return NewIssue(
        title = draft.title.trim(),
        body = if (detail.isEmpty()) footer else "$detail\n\n$footer",
        labels = listOf(draft.kind.githubLabel),
    )
}

/** Reads the number and page URL from a successful create-issue response. */
fun parseCreatedIssue(json: String): CreatedIssue =
    lenientJson.decodeFromString<CreatedIssueResponse>(json).let { CreatedIssue(it.number, it.htmlUrl) }

/** GitHub's error message from a failed response, or the raw text when it is not the usual JSON. */
fun parseErrorMessage(json: String): String =
    runCatching { lenientJson.parseToJsonElement(json).jsonObject["message"]?.jsonPrimitive?.content }
        .getOrNull() ?: json.trim()
