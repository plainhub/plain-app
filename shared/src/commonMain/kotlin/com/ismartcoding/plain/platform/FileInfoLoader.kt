package com.ismartcoding.plain.platform

import com.ismartcoding.plain.web.models.AudioFileInfo
import com.ismartcoding.plain.web.models.ImageFileInfo
import com.ismartcoding.plain.web.models.VideoFileInfo

/**
 * Loads image metadata (dimensions and EXIF GPS location) from [path].
 * SVG files have no location and use a separate decoding path.
 */
expect fun loadImageInfo(path: String): ImageFileInfo

/**
 * Loads video metadata (width, height, duration in seconds, GPS location) from [path].
 */
expect fun loadVideoInfo(path: String): VideoFileInfo

/**
 * Loads audio metadata (duration in seconds, GPS location) from [path].
 */
expect fun loadAudioInfo(path: String): AudioFileInfo
