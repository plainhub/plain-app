package com.ismartcoding.plain.web.models

import kotlinx.datetime.Instant
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
    val tags: List<Tag>,
    @Contextual var data: MediaFileInfo?,
)

@Polymorphic
@Serializable
sealed class MediaFileInfo

data class ImageFileInfo(val width: Int, val height: Int, val location: Location?) : MediaFileInfo()

data class AudioFileInfo(val duration: Long, val location: Location?) : MediaFileInfo()

data class VideoFileInfo(val width: Int, val height: Int, val duration: Long, val location: Location?) : MediaFileInfo()
