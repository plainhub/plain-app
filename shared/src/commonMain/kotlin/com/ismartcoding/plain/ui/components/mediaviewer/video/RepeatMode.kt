package com.ismartcoding.plain.ui.components.mediaviewer.video

enum class RepeatMode(val value: String) {
    NONE("none"),
    ONE("one"),
    ALL("all"),
}

fun Int.toRepeatMode(): RepeatMode =
        when (this) {
            0 -> RepeatMode.NONE
            1 -> RepeatMode.ONE
            2 -> RepeatMode.ALL
            else -> RepeatMode.NONE
        }
