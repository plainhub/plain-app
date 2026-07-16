package com.ismartcoding.plain.platform

import com.ismartcoding.plain.web.models.AudioFileInfo
import com.ismartcoding.plain.web.models.ImageFileInfo
import com.ismartcoding.plain.web.models.VideoFileInfo

actual fun loadImageInfo(path: String): ImageFileInfo =
    ImageFileInfo(0, 0, null)

actual fun loadVideoInfo(path: String): VideoFileInfo =
    VideoFileInfo(0, 0, 0L, null)

actual fun loadAudioInfo(path: String): AudioFileInfo =
    AudioFileInfo(0L, null)
