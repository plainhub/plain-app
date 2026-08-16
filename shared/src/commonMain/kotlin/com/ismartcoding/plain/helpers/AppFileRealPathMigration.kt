package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.appDir
import com.ismartcoding.plain.lib.withIO

/**
 * One-time migration: strip the platform-specific `appDir()/` prefix from
 * `app_files.real_path` rows that still store absolute paths (legacy data).
 *
 * New rows store relative paths (`{aa}/{bb}/{name}`); this migration brings
 * legacy rows in line so the column doesn't waste space on the repeated
 * platform-specific prefix. Idempotent — rows already storing a relative
 * path (not starting with `/`) are left untouched.
 *
 * Guarded by [com.ismartcoding.plain.preferences.AppFileRealPathMigratedPreference]
 * so it only runs once.
 */
object AppFileRealPathMigration {

    suspend fun run() = withIO {
        val dir = appDir() + "/"
        val dao = AppDatabase.instance.appFileDao()
        var changed = 0
        dao.getAll().forEach { file ->
            if (file.realPath.startsWith(dir)) {
                file.realPath = file.realPath.removePrefix(dir)
                dao.update(file)
                changed += 1
            }
        }
        LogCat.d("AppFileRealPathMigration: done, migrated $changed rows")
    }
}
