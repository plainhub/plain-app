package com.ismartcoding.lib.media

interface IMediaPlayer {
    fun play()

    fun pause()

    fun stop()

    fun isPlaying(): Boolean

    fun setVolume(volume: Float)

    var isPausedByTransientLossOfFocus: Boolean
}
