package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Audio(
    override val id: ID,
    override val title: String,
    val artist: String,
    override val path: String,
    val durationMs: Long,
    override val size: Long,
    override val bucketId: ID,
    @GraphQLField(description = "FileId of the album-art image, usable in file display URLs; empty when the track has no album art.")
    val albumFileId: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val isFavorite: Boolean,
) : MediaItem

@GraphQLType
data class AudioItem(
    val title: String,
    val artist: String,
    val path: String,
    val durationMs: Long,
)

@GraphQLType
data class AudioPlaylist(
    val id: ID,
    val name: String,
    val itemCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@GraphQLType
data class AudioPlayHistory(
    val path: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val playCount: Long,
    val playedAt: Instant,
)
