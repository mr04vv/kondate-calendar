package com.github.mr04vv.kondatecalendar.feedback

import platform.UIKit.UIDevice

actual fun deviceDescription(): String = UIDevice.currentDevice.let { "${it.model} / ${it.systemName} ${it.systemVersion}" }
