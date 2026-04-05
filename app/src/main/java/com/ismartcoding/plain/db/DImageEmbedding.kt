package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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
