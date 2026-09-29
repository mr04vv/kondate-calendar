package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

// ponytail: photos are Android-only for now, so an iOS dish never has one. Add a PHPickerViewController-based
// PhotoSection and decode with Skia's Image.makeFromEncoded if the iPad needs photos.
actual fun loadPhoto(path: String): ImageBitmap? = null

@OptIn(ExperimentalForeignApi::class)
actual fun deletePhoto(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, error = null)
}

@Composable
actual fun PhotoSection(photoPath: String?, onChange: (String?) -> Unit) = Unit
