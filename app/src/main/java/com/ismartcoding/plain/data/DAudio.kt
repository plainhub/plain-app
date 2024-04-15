package com.ismartcoding.plain.data

data class DAudio(
    override var id: String,
    val title: String,
    val artist: String,
    override val path: String,
    override val duration: Long,
    val size: Long,
    val bucketId: String,
) : IData, IMedia {
    fun toPlaylistAudio(): DPlaylistAudio {
        return DPlaylistAudio(title, path, artist, duration)
    }
}
