package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.ui.geometry.Offset

const val DEFAULT_OFFSET_X = 0F
const val DEFAULT_OFFSET_Y = 0F
const val DEFAULT_SCALE = 1F
const val DEFAULT_ROTATION = 0F
const val MIN_SCALE = 0.5F
const val MAX_SCALE_RATE = 3.2F
const val MIN_GESTURE_FINGER_DISTANCE = 200

class GestureScope(
    var onTap: (Offset) -> Unit = {},
    var onDoubleTap: (Offset) -> Boolean = { false },
    var onLongPress: (Offset) -> Unit = {},
)
