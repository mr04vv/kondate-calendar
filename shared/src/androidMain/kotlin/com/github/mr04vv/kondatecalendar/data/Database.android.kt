package com.github.mr04vv.kondatecalendar.data

import android.content.Context
import androidx.room.Room
import java.io.File
import java.io.IOException

/** Opens the same file the Android-only builder used, so existing data carries over. */
fun createDatabase(context: Context): KondateDatabase {
    val app = context.applicationContext
    return KondateDatabase.build(Room.databaseBuilder<KondateDatabase>(app, app.getDatabasePath(KondateDatabase.NAME).absolutePath))
}

fun legacyDatabase(context: Context): LegacyDatabase {
    val app = context.applicationContext
    return LegacyDatabase(app.getDatabasePath(KondateDatabase.NAME).absolutePath) { createDatabase(app) }
}

actual fun fileExists(path: String): Boolean = File(path).exists()

actual fun moveFile(from: String, to: String) {
    if (!File(from).renameTo(File(to))) throw IOException("could not rename $from to $to")
}
