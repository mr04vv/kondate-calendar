package com.github.mr04vv.kondatecalendar

import android.app.Application
import com.github.mr04vv.kondatecalendar.data.KondateDatabase
import com.github.mr04vv.kondatecalendar.data.createDatabase

class KondateApp : Application() {
    val db: KondateDatabase by lazy { createDatabase(this) }
}
