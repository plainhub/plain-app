package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query

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
