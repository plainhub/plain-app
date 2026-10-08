package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.system.AudioInfoFacts
import com.ismartcoding.plain.features.system.ImageInfoFacts
import com.ismartcoding.plain.features.system.VideoInfoFacts

/**
 * Loads image metadata (dimensions and EXIF GPS location) from [path].
 * SVG files have no location and use a separate decoding path.
 */
expect fun loadImageInfo(path: String): ImageInfoFacts

/**
 * Loads video metadata (width, height, duration in seconds, GPS location) from [path].
 */
expect fun loadVideoInfo(path: String): VideoInfoFacts

/**
 * Loads audio metadata (duration in seconds, GPS location) from [path].
 */
expect fun loadAudioInfo(path: String): AudioInfoFacts
