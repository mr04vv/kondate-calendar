package com.github.mr04vv.kondatecalendar.data

import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createDatabase(): KondateDatabase {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val path = requireNotNull(documents?.path) { "no documents directory" }
    return KondateDatabase.build(Room.databaseBuilder<KondateDatabase>(name = "$path/${KondateDatabase.NAME}"))
}
