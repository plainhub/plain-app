package com.ismartcoding.plain.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * iOS actual: builds the [AppDatabase] using the Room KMP API with
 * [BundledSQLiteDriver] and registers all manual migrations.
 * Instantiation is handled by [AppDatabaseConstructor] (declared via
 * `@ConstructedBy` in commonMain), so no explicit factory is needed.
 * All other database logic (entities, DAOs, auto-migrations, data
 * initializer) lives in commonMain.
 */
actual fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
        name = databaseFilePath(name),
    )
        .setDriver(BundledSQLiteDriver())
        .addAllMigrations()
}
