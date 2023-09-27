package com.ismartcoding.plain.features.audio

enum class AudioAction {
    COMPLETE,
    PLAY,
    PAUSE,
    STOP,
    SEEK,
    NOT_FOUND,
    NOTIFICATION,
}

enum class AudioServiceAction {
    PLAY,
    PAUSE,
    SKIP_NEXT,
    SKIP_PREVIOUS,
    SEEK,
    QUIT,
    PENDING_QUIT,
}
