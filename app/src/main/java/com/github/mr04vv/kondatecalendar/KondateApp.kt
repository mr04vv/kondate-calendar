package com.github.mr04vv.kondatecalendar

import android.app.Application
import com.github.mr04vv.kondatecalendar.data.KondateDatabase

class KondateApp : Application() {
    val db: KondateDatabase by lazy { KondateDatabase.create(this) }
}
