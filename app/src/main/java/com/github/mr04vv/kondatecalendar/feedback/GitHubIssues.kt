package com.github.mr04vv.kondatecalendar.feedback

import com.github.mr04vv.kondatecalendar.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class IssueSendException(message: String) : IOException(message)

object GitHubIssues {
    private const val API = "https://api.github.com/repos"
    private const val TIMEOUT_MS = 15_000
    private const val API_VERSION = "2022-11-28"

    val isConfigured: Boolean get() = BuildConfig.GITHUB_ISSUE_TOKEN.isNotBlank()

    /** Creates [issue] in the app's repository. Throws [IOException] on network or API failure. */
    suspend fun create(issue: NewIssue): CreatedIssue = withContext(Dispatchers.IO) {
        val connection = URL("$API/${BuildConfig.GITHUB_REPO}/issues").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_ISSUE_TOKEN}")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", API_VERSION)
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(Json.encodeToString(issue).toByteArray()) }
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_CREATED) {
                parseCreatedIssue(connection.inputStream.bufferedReader().use { it.readText() })
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IssueSendException("HTTP $code: ${parseErrorMessage(error)}")
            }
        } finally {
            connection.disconnect()
        }
    }
}
