package com.ismartcoding.plain.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

@PublishedApi
internal var appContextValue: Context? = null

fun setAppContext(context: Context) {
    appContextValue = context
}

actual fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase> {
    val ctx = appContextValue ?: error("setAppContext must be called before buildAppDatabase")
    return Room.databaseBuilder<AppDatabase>(ctx, name)
        .addMigrations(Migrations.MIGRATION_5_6)
}