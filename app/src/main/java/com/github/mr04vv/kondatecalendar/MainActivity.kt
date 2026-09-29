package com.github.mr04vv.kondatecalendar

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.mr04vv.kondatecalendar.ui.KondateAppUi
import com.github.mr04vv.kondatecalendar.ui.KondateTheme
import com.github.mr04vv.kondatecalendar.ui.KondateViewModel

class MainActivity : ComponentActivity() {
    private val vm: KondateViewModel by viewModels {
        viewModelFactory { initializer { KondateViewModel((application as KondateApp).db.dao()) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always light, so keep dark system-bar icons even when the device is in dark mode.
        val lightBars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        super.onCreate(savedInstanceState)
        setContent {
            // The system back gesture closes pushed screens; on iOS their top-bar arrows do.
            BackHandler(enabled = vm.stack.isNotEmpty()) { vm.back() }
            KondateTheme { KondateAppUi(vm) }
        }
    }
}
