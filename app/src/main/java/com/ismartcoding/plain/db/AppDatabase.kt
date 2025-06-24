package com.ismartcoding.plain.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database

import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.MainApp

@DeleteTable(tableName = "boxes")
class BoxesDeletionSpec : AutoMigrationSpec

@DeleteTable(tableName = "aichats")
class AiChatsDeletionSpec : AutoMigrationSpec

@Database(
    entities = [
        DChat::class, DSession::class, DTag::class, DTagRelation::class,
        DNote::class, DFeed::class, DFeedEntry::class, DBook::class, DBookChapter::class,
        DPomodoroItem::class, DPeer::class, DChatGroup::class,
    ],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = BoxesDeletionSpec::class),
        AutoMigration(from = 3, to = 4, spec = AiChatsDeletionSpec::class),
        AutoMigration(from = 4, to = 5)
    ],
    exportSchema = true,
)
@TypeConverters(DateConverter::class, StringListConverter::class, ChatItemContentConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    abstract fun sessionDao(): SessionDao

    abstract fun tagDao(): TagDao

    abstract fun tagRelationDao(): TagRelationDao

    abstract fun noteDao(): NoteDao

    abstract fun feedDao(): FeedDao

    abstract fun feedEntryDao(): FeedEntryDao

    abstract fun bookDao(): BookDao

    abstract fun pomodoroItemDao(): PomodoroItemDao

    abstract fun peerDao(): PeerDao

    abstract fun chatGroupDao(): ChatGroupDao


    companion object {
        @Volatile
        private var _instance: AppDatabase? = null

        val instance: AppDatabase
            get() {
                return _instance ?: synchronized(this) {
                    _instance ?: buildDatabase(MainApp.instance).also { _instance = it }
                }
            }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME)
                .addMigrations(Migrations.MIGRATION_5_6)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            DataInitializer(context, db).apply {
                                insertWelcome()
                                insertTags()
                                insertNotes()
                            }
                        }
                    },
                )
                .build()
        }
    }
}
