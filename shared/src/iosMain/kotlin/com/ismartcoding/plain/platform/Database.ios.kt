package com.ismartcoding.plain.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ismartcoding.plain.db.Migrations

/**
 * iOS actual: builds the [AppDatabase] using the Room KMP API with
 * [BundledSQLiteDriver] and registers the manual 5→6 migration.
 * All other database logic (entities, DAOs, auto-migrations, data initializer)
 * lives in commonMain.
 */
actual fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
        name = databaseFilePath(name),
        factory = { AppDatabaseConstructor.initialize() },
    )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(Migrations.MIGRATION_5_6)
}
