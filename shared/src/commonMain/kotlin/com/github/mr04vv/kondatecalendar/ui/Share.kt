package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.runtime.Composable

/** Returns a function that hands plain text to the platform share sheet. */
@Composable
expect fun rememberShareText(): (String) -> Unit
