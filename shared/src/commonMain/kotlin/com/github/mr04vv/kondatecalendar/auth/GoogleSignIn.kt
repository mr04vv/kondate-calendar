package com.github.mr04vv.kondatecalendar.auth

/** Tokens from the platform's Google sign-in, exchanged for a Firebase session. */
class GoogleTokens(val idToken: String, val accessToken: String?)

/** Shows the platform's Google account chooser; returns null when the user backs out. */
typealias GoogleSignIn = suspend () -> GoogleTokens?
