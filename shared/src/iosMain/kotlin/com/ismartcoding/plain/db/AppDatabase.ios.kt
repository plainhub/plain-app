package com.ismartcoding.plain.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase> {
    val docDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0] as String
    val dbFilePath = "$docDir/$name"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
        factory = { AppDatabase::class.instantiateImpl() },
    ).setDriver(BundledSQLiteDriver())
}