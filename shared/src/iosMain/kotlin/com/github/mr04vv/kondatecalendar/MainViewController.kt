package com.github.mr04vv.kondatecalendar

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.github.mr04vv.kondatecalendar.data.createDatabase
import com.github.mr04vv.kondatecalendar.ui.KondateAppUi
import com.github.mr04vv.kondatecalendar.ui.KondateTheme
import com.github.mr04vv.kondatecalendar.ui.KondateViewModel
import platform.UIKit.UIViewController

/** The iOS app's root view, embedded by iosApp. */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
    val vm = remember { KondateViewModel(createDatabase().dao()) }
    KondateTheme { KondateAppUi(vm) }
}
