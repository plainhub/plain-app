package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query

@Entity(tableName = "trashed_messages")
data class DTrashedMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String, // sms numeric id, or "mms_<id>" for MMS
    @ColumnInfo(name = "is_mms")
    val isMms: Boolean,
    @ColumnInfo(name = "trashed_at")
    val trashedAt: Long, // epoch millis; used for the 30-day retention cleanup
)

@Dao
interface TrashedMessageDao {
    @Query("SELECT message_id FROM trashed_messages")
    suspend fun getAllIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DTrashedMessage>)

    @Query("DELETE FROM trashed_messages WHERE message_id IN (:messageIds)")
    suspend fun deleteByMessageIds(messageIds: List<String>)

    @Query("DELETE FROM trashed_messages WHERE trashed_at < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)
}
