package com.github.mr04vv.kondatecalendar.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** The Firebase project's web client id, generated from google-services.json by the Google services plugin. */
private const val WEB_CLIENT_ID_RESOURCE = "default_web_client_id"

/** Signs in through Credential Manager; [activity] hosts the account chooser. */
fun googleSignIn(activity: Activity): GoogleSignIn = signIn@{
    val resource = activity.resources.getIdentifier(WEB_CLIENT_ID_RESOURCE, "string", activity.packageName)
    check(resource != 0) { "google-services.json が app/ にありません" }
    val option = GetSignInWithGoogleOption.Builder(activity.getString(resource)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val result = try {
        CredentialManager.create(activity).getCredential(activity, request)
    } catch (_: GetCredentialCancellationException) {
        return@signIn null
    }
    GoogleTokens(GoogleIdTokenCredential.createFrom(result.credential.data).idToken, accessToken = null)
}
