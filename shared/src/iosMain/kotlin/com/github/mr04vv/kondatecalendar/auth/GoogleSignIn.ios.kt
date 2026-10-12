package com.github.mr04vv.kondatecalendar.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The Swift host's GoogleSignIn call, which Kotlin cannot reach directly. It calls back with the id and access
 * tokens, with nothing when the user backs out, or with an error message.
 */
typealias SwiftGoogleSignIn = (callback: (idToken: String?, accessToken: String?, error: String?) -> Unit) -> Unit

fun googleSignIn(swift: SwiftGoogleSignIn): GoogleSignIn = {
    suspendCancellableCoroutine { continuation ->
        swift { idToken, accessToken, error ->
            when {
                error != null -> continuation.resumeWithException(IllegalStateException(error))
                idToken == null -> continuation.resume(null)
                else -> continuation.resume(GoogleTokens(idToken, accessToken))
            }
        }
    }
}
