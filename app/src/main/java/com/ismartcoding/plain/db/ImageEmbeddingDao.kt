package com.ismartcoding.plain.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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
