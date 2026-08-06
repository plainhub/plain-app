package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLUnion
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@GraphQLType
data class Location(
    val latitude: Double,
    val longitude: Double,
)

@GraphQLType
@Polymorphic
@Serializable
class FileInfo(
    val path: String,
    val updatedAt: Instant,
    val size: Long,
    val tags: List<Tag>,
    @Contextual var data: MediaFileInfo?,
)

@GraphQLUnion
@Polymorphic
@Serializable
sealed class MediaFileInfo

@GraphQLType
data class ImageFileInfo(val width: Int, val height: Int, val location: Location?) : MediaFileInfo()

@GraphQLType
data class AudioFileInfo(val duration: Long, val location: Location?) : MediaFileInfo()

@GraphQLType
data class VideoFileInfo(val width: Int, val height: Int, val duration: Long, val location: Location?) : MediaFileInfo()
