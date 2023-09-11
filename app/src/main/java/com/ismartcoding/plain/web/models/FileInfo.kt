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
open class FileInfo(
    open val updatedAt: Instant,
    open val size: Long,
    @Contextual var data: MediaFileInfo? = null,
)

@Polymorphic
@Serializable
sealed class MediaFileInfo

data class ImageFileInfo(val tags: List<Tag>, val width: Int, val height: Int, val location: Location?) : MediaFileInfo()

data class AudioFileInfo(val tags: List<Tag>, val duration: Long, val location: Location?) : MediaFileInfo()

data class VideoFileInfo(val tags: List<Tag>, val width: Int, val height: Int, val duration: Long, val location: Location?) : MediaFileInfo()