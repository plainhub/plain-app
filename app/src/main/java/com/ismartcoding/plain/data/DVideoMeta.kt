package com.ismartcoding.plain.data

import kotlinx.datetime.Instant

data class DVideoMeta(
    val width: Int,
    val height: Int,
    val rotation: Int,
    val duration: Long,
    val bitrate: Long,
    val frameRate: Float,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val takenAt: Instant?,
    val writer: String,
    val composer: String,
)