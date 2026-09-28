package com.github.mr04vv.kondatecalendar.feedback

import com.github.mr04vv.kondatecalendar.BuildInfo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

class IssueSendException(message: String) : IOException(message)

object GitHubIssues {
    private const val API = "https://api.github.com/repos"
    private const val TIMEOUT_MS = 15_000L
    private const val API_VERSION = "2022-11-28"

    private val client by lazy { HttpClient { install(HttpTimeout) { requestTimeoutMillis = TIMEOUT_MS } } }

    val isConfigured: Boolean get() = BuildInfo.GITHUB_ISSUE_TOKEN.isNotBlank()

    /** Creates [issue] in the app's repository. Throws [IOException] on network or API failure. */
    suspend fun create(issue: NewIssue): CreatedIssue {
        val response = client.post("$API/${BuildInfo.GITHUB_REPO}/issues") {
            header(HttpHeaders.Authorization, "Bearer ${BuildInfo.GITHUB_ISSUE_TOKEN}")
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header("X-GitHub-Api-Version", API_VERSION)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(issue))
        }
        val body = response.bodyAsText()
        if (response.status != HttpStatusCode.Created) {
            throw IssueSendException("HTTP ${response.status.value}: ${parseErrorMessage(body)}")
        }
        return parseCreatedIssue(body)
    }
}
