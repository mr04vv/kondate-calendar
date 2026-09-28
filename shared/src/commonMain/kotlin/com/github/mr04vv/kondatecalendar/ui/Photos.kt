package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Decodes the photo at [path] for display, or null when it cannot be read. */
expect fun loadPhoto(path: String): ImageBitmap?

expect fun deletePhoto(path: String)

/** The edit screen's photo controls: shows [photoPath] and lets the user take, pick or remove one. */
@Composable
expect fun PhotoSection(photoPath: String?, onChange: (String?) -> Unit)
