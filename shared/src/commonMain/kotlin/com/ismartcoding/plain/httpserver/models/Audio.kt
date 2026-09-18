package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Audio(
    val id: ID,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val bucketId: String,
    val albumFileId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isFavorite: Boolean,
)

@GraphQLType
data class PlaylistAudio(
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
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
    val duration: Long,
    val playCount: Long,
    val playedAt: Instant,
)
