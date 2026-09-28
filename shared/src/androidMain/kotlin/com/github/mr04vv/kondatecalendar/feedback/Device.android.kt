package com.github.mr04vv.kondatecalendar.feedback

import android.os.Build

actual fun deviceDescription(): String = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}"
