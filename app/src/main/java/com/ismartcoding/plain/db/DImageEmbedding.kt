package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_embeddings")
data class DImageEmbedding(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DImageEmbedding) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
