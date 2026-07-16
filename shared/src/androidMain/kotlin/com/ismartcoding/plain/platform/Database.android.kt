package com.ismartcoding.plain.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.db.Migrations

/**
 * Android actual: builds the [AppDatabase] using the Android Room API
 * (which requires a [android.content.Context]) and registers the manual
 * 5→6 migration. All other database logic (entities, DAOs, migrations,
 * data initializer) lives in commonMain.
 */
actual fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(appContext, name)
        .addMigrations(Migrations.MIGRATION_5_6)
}
