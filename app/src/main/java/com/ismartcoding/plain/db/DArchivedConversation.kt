package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "archived_conversations")
data class DArchivedConversation(
    @PrimaryKey
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "conversation_date")
    val conversationDate: Long, // epoch millis when archived; messages before this date are archived
)

@Dao
interface ArchivedConversationDao {
    @Query("SELECT * FROM archived_conversations")
    suspend fun getAll(): List<DArchivedConversation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DArchivedConversation)

    @Query("DELETE FROM archived_conversations WHERE conversation_id = :conversationId")
    suspend fun delete(conversationId: String)
}
