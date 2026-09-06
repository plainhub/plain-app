package com.ismartcoding.plain.platform

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * iOS actual: builds the [AppDatabase] using the Room KMP API with
 * [BundledSQLiteDriver] and registers all manual migrations.
 * Instantiation is handled by [AppDatabaseConstructor] (declared via
 * `@ConstructedBy` in commonMain), so no explicit factory is needed.
 * All other database logic (entities, DAOs, auto-migrations, data
 * initializer) lives in commonMain.
 */
private fun databaseFilePath(name: String): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0] as String + "/" + name

actual fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
        name = databaseFilePath(name),
    )
        .setDriver(BundledSQLiteDriver())
        .setSingleConnectionPool()
        .addAllMigrations()
}
