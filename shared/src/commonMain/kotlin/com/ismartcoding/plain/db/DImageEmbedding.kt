package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.ismartcoding.plain.data.IData

@Entity(tableName = "image_embeddings")
data class DImageEmbedding(
    @PrimaryKey
    override var id: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray,
) : IData, DEntityBase()

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
