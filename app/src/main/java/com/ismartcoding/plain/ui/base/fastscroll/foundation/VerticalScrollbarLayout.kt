package com.ismartcoding.plain.ui.base.fastscroll.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp

private val THUMB_WIDTH = 4.dp
private val THUMB_HEIGHT = 40.dp
private val TRACK_WIDTH = 4.dp
private val TOUCH_AREA_WIDTH = 20.dp
private val CORNER_RADIUS = 2.dp

@Composable
internal fun VerticalScrollbarLayout(
    thumbOffsetNormalized: Float,
    thumbIsInAction: Boolean,
    thumbIsSelected: Boolean,
    settings: ScrollbarLayoutSettings,
    draggableModifier: Modifier,
    indicator: (@Composable () -> Unit)?,
) {
    val state = rememberScrollbarLayoutState(
        thumbIsInAction = thumbIsInAction,
        thumbIsSelected = thumbIsSelected,
        settings = settings,
    )

    val trackAlpha by animateFloatAsState(
        targetValue = if (thumbIsSelected) 1f else 0f,
        animationSpec = tween(durationMillis = if (thumbIsSelected) 100 else 250),
        label = "track alpha",
    )

    Layout(
        content = {
            // Track: full-height gray strip shown when dragging
            Box(
                modifier = Modifier
                    .alpha(trackAlpha)
                    .fillMaxHeight()
                    .width(TRACK_WIDTH)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(CORNER_RADIUS),
                    ),
            )

            // Thumb: small primary-color pill
            Box(
                modifier = Modifier
                    .alpha(state.hideAlpha.value)
                    .width(THUMB_WIDTH)
                    .height(THUMB_HEIGHT)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(CORNER_RADIUS),
                    )
                    .run { if (state.activeDraggableModifier.value) then(draggableModifier) else this },
            )

            // Indicator: optional, shown beside the thumb
            when (indicator) {
                null -> Box(Modifier)
                else -> Box(Modifier.alpha(state.hideAlpha.value)) { indicator() }
            }

            // Invisible touch area covering full height
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(TOUCH_AREA_WIDTH)
                    .run { if (state.activeDraggableModifier.value) then(draggableModifier) else this },
            )
        },
        measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                val placeableTrack = placeables[0]
                val placeableThumb = placeables[1]
                val placeableIndicator = placeables[2]
                val placeableScrollbarArea = placeables[3]

                val offset = (constraints.maxHeight.toFloat() * thumbOffsetNormalized).toInt()
                val hideDisplacementPx = state.hideDisplacement.value.roundToPx()
                val rightX = constraints.maxWidth - THUMB_WIDTH.roundToPx() + hideDisplacementPx

                placeableTrack.placeRelative(x = rightX, y = 0)

                placeableThumb.placeRelative(x = rightX, y = offset)

                placeableIndicator.placeRelative(
                    x = rightX - placeableIndicator.width,
                    y = offset + placeableThumb.height / 2 - placeableIndicator.height / 2,
                )

                placeableScrollbarArea.placeRelative(
                    x = constraints.maxWidth - placeableScrollbarArea.width,
                    y = 0,
                )
            }
        },
    )
}
