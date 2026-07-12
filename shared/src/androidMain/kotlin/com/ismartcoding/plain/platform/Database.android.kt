package com.ismartcoding.plain.platform

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ismartcoding.plain.db.Migrations

@PublishedApi
internal var databaseContextValue: Context? = null

fun setDatabaseContext(context: Context) {
    databaseContextValue = context
}

actual fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase> {
    val ctx = databaseContextValue ?: error("setDatabaseContext must be called before buildAppDatabase")
    return Room.databaseBuilder<AppDatabase>(ctx, name)
        .addMigrations(Migrations.MIGRATION_5_6)
}
