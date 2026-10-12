package com.github.mr04vv.kondatecalendar.data

import androidx.room.Room
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun databasePath(): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val path = requireNotNull(documents?.path) { "no documents directory" }
    return "$path/${KondateDatabase.NAME}"
}

fun createDatabase(): KondateDatabase = KondateDatabase.build(Room.databaseBuilder<KondateDatabase>(name = databasePath()))

fun legacyDatabase(): LegacyDatabase = LegacyDatabase(databasePath(), ::createDatabase)

actual fun fileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun moveFile(from: String, to: String) {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val moved = NSFileManager.defaultManager.moveItemAtPath(from, toPath = to, error = error.ptr)
        if (!moved) throw IllegalStateException("could not rename $from to $to: ${error.value?.localizedDescription}")
    }
}
