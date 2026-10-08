package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.system.AudioInfoFacts
import com.ismartcoding.plain.features.system.ImageInfoFacts
import com.ismartcoding.plain.features.system.VideoInfoFacts

actual fun loadImageInfo(path: String): ImageInfoFacts =
    ImageInfoFacts(0, 0, null)

actual fun loadVideoInfo(path: String): VideoInfoFacts =
    VideoInfoFacts(0, 0, 0L, null)

actual fun loadAudioInfo(path: String): AudioInfoFacts =
    AudioInfoFacts(0L, null)
