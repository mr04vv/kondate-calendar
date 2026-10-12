package com.github.mr04vv.kondatecalendar.data

import com.github.mr04vv.kondatecalendar.domain.MigrationData
import com.github.mr04vv.kondatecalendar.domain.migrationData

/**
 * The local database file from before the data moved to the server. Nothing writes to it any more; it is read once
 * and then set aside under a new name, never deleted, so the data can still be recovered if a move goes wrong.
 */
class LegacyDatabase(private val path: String, private val open: () -> KondateDatabase) {
    fun exists(): Boolean = fileExists(path)

    /** What the file holds, converted for the server; see [migrationData]. */
    suspend fun read(presets: List<Dish>): MigrationData {
        val db = open()
        try {
            val dao = db.dao()
            return migrationData(dao.dishes(), dao.slots(), dao.shoppingItems(), presets)
        } finally {
            db.close()
        }
    }

    /** Renames the file (and SQLite's journal files beside it) so it is not offered again. */
    fun setAside() {
        JOURNAL_SUFFIXES.forEach { suffix ->
            if (fileExists(path + suffix)) moveFile(path + suffix, path + SET_ASIDE_SUFFIX + suffix)
        }
    }

    private companion object {
        /** The database itself and the write-ahead log files SQLite keeps beside it. */
        val JOURNAL_SUFFIXES = listOf("", "-wal", "-shm")
        const val SET_ASIDE_SUFFIX = ".migrated"
    }
}

expect fun fileExists(path: String): Boolean

/** Renames [from] to [to]; throws when that fails. */
expect fun moveFile(from: String, to: String)
