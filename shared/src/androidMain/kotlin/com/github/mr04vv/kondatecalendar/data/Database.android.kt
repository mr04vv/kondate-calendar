package com.github.mr04vv.kondatecalendar.data

import android.content.Context
import androidx.room.Room

/** Opens the same file the Android-only builder used, so existing data carries over. */
fun createDatabase(context: Context): KondateDatabase {
    val app = context.applicationContext
    return KondateDatabase.build(Room.databaseBuilder<KondateDatabase>(app, app.getDatabasePath(KondateDatabase.NAME).absolutePath))
}
