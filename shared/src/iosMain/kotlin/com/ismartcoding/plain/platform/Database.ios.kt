package com.ismartcoding.plain.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase> {
    val docDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0] as String
    val dbFilePath = "$docDir/$name"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
        factory = { AppDatabaseConstructor.initialize() },
    ).setDriver(BundledSQLiteDriver())
}
