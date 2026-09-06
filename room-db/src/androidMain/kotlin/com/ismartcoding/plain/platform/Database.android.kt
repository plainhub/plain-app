package com.ismartcoding.plain.platform

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ismartcoding.plain.appContext

/**
 * Android actual: builds the [AppDatabase] using the Android Room API
 * (which requires a [android.content.Context]) and registers all manual migrations.
 * Database instantiation is handled by [AppDatabaseConstructor]
 * (declared via `@ConstructedBy` in commonMain). All other database logic
 * (entities, DAOs, auto-migrations, data initializer) lives in commonMain.
 */
actual fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(appContext, name)
        .setDriver(BundledSQLiteDriver())
        .addAllMigrations()
}
