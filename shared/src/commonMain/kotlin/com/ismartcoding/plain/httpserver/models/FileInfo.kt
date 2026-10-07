package com.ismartcoding.plain.httpserver.models

import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

data class Location(
    val latitude: Double,
    val longitude: Double,
)

@Polymorphic
@Serializable
class FileInfo(
    val path: String,
    val updatedAt: Instant,
    val size: Long,
    @Contextual var data: MediaFileInfo?,
)

@Polymorphic
@Serializable
sealed class MediaFileInfo

data class ImageFileInfo(val width: Int, val height: Int, val location: Location?) : MediaFileInfo()

data class AudioFileInfo(val durationMs: Long, val location: Location?) : MediaFileInfo()

data class VideoFileInfo(val width: Int, val height: Int, val durationMs: Long, val location: Location?) : MediaFileInfo()
