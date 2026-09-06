package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import com.ismartcoding.plain.db.IData

@Entity(tableName = "image_embeddings")
data class DImageEmbedding(
    @PrimaryKey
    override var id: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray,

    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now(),

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now(),
) : IData

@Dao
interface ImageEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(embedding: DImageEmbedding)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embeddings: List<DImageEmbedding>)

    @Query("SELECT * FROM image_embeddings")
    suspend fun getAll(): List<DImageEmbedding>

    @Query("SELECT id FROM image_embeddings")
    suspend fun getAllIds(): List<String>

    @Query("SELECT COUNT(*) FROM image_embeddings")
    suspend fun count(): Int

    @Query("DELETE FROM image_embeddings")
    suspend fun deleteAll()

    @Query("DELETE FROM image_embeddings WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
