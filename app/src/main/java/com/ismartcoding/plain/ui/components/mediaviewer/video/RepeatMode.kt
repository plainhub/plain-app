package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.media3.common.Player

enum class RepeatMode(val value: String) {
    NONE("none"),
    ONE("one"),
    ALL("all"),
}

internal fun RepeatMode.toExoPlayerRepeatMode(): Int =
    when (this) {
        RepeatMode.NONE -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

fun Int.toRepeatMode(): RepeatMode =
        when (this) {
            0 -> RepeatMode.NONE
            1 -> RepeatMode.ONE
            2 -> RepeatMode.ALL
            else -> RepeatMode.NONE
        }
