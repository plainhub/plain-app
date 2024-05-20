package com.ismartcoding.plain.data

data class DVideoMeta(
    val width: Int,
    val height: Int,
    val rotation: Int,
    val duration: Long,
    val bitrate: Int,
    val frameRate: Float,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val date: String,
    val writer: String,
    val composer: String,
)