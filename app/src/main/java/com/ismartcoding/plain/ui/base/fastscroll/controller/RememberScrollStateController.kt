package com.ismartcoding.plain.ui.base.fastscroll.controller

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSelectionMode

@Composable
internal fun rememberScrollStateController(
    state: ScrollState,
    visibleLengthDp: Dp,
    thumbMinLength: Float,
    alwaysShowScrollBar: Boolean,
    selectionMode: ScrollbarSelectionMode
): ScrollStateController {
    val coroutineScope = rememberCoroutineScope()

    val visibleLengthDpUpdated = rememberUpdatedState(visibleLengthDp)
    val thumbMinLengthUpdated = rememberUpdatedState(thumbMinLength)
    val alwaysShowScrollBarUpdated = rememberUpdatedState(alwaysShowScrollBar)
    val selectionModeUpdated = rememberUpdatedState(selectionMode)

    val isSelected = remember { mutableStateOf(false) }
    val dragOffset = remember { mutableFloatStateOf(0f) }

    val fullLengthDp = with(LocalDensity.current) {
        remember {
            derivedStateOf {
                state.maxValue.toDp() + visibleLengthDpUpdated.value
            }
        }
    }

    val thumbSizeNormalizedReal = remember {
        derivedStateOf {
            if (fullLengthDp.value == 0.dp) 1f else {
                val normalizedDp = visibleLengthDpUpdated.value / fullLengthDp.value
                normalizedDp.coerceIn(0f, 1f)
            }
        }
    }

    val thumbSizeNormalized = remember {
        derivedStateOf {
            thumbSizeNormalizedReal.value.coerceAtLeast(thumbMinLengthUpdated.value)
        }
    }

    fun offsetCorrection(top: Float): Float {
        val topRealMax = 1f
        val topMax = (1f - thumbSizeNormalized.value).coerceIn(0f, 1f)
        return top * topMax / topRealMax
    }

    val thumbOffsetNormalized = remember {
        derivedStateOf {
            if (state.maxValue == 0) return@derivedStateOf 0f
            val normalized = state.value.toFloat() / state.maxValue.toFloat()
            offsetCorrection(normalized)
        }
    }

    val thumbIsInAction = remember {
        derivedStateOf {
            state.isScrollInProgress || isSelected.value || alwaysShowScrollBarUpdated.value
        }
    }

    return remember {
        ScrollStateController(
            thumbSizeNormalized = thumbSizeNormalized,
            thumbOffsetNormalized = thumbOffsetNormalized,
            thumbIsInAction = thumbIsInAction,
            _isSelected = isSelected,
            dragOffset = dragOffset,
            state = state,
            selectionMode = selectionModeUpdated,
            coroutineScope = coroutineScope
        )
    }
}
