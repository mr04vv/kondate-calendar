package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberShareText(): (String) -> Unit {
    val controller = LocalUIViewController.current
    return remember(controller) {
        { text ->
            val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            // On iPad the sheet is a popover and needs an anchor; center it without an arrow.
            sheet.popoverPresentationController?.let { popover ->
                popover.sourceView = controller.view
                popover.sourceRect = controller.view.bounds.useContents { CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0) }
                popover.permittedArrowDirections = 0u
            }
            controller.presentViewController(sheet, animated = true, completion = null)
        }
    }
}
