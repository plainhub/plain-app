package com.ismartcoding.plain.ui.base.fastscroll.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarLayoutSide

@Composable
internal fun HorizontalScrollbarLayout(
    thumbSizeNormalized: Float,
    thumbOffsetNormalized: Float,
    thumbIsInAction: Boolean,
    thumbIsSelected: Boolean,
    settings: ScrollbarLayoutSettings,
    draggableModifier: Modifier,
    indicator: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val state = rememberScrollbarLayoutState(
        thumbIsInAction = thumbIsInAction,
        thumbIsSelected = thumbIsSelected,
        settings = settings,
    )

    Layout(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(thumbSizeNormalized)
                    .padding(
                        top = if (settings.side == ScrollbarLayoutSide.Start) settings.scrollbarPadding else 0.dp,
                        bottom = if (settings.side == ScrollbarLayoutSide.End) settings.scrollbarPadding else 0.dp,
                    )
                    .alpha(state.hideAlpha.value)
                    .clip(settings.thumbShape)
                    .height(settings.thumbThickness)
                    .background(state.thumbColor.value)
            )
            when (indicator) {
                null -> Box(Modifier)
                else -> Box(
                    Modifier
                        .alpha(state.hideAlpha.value)
                ) {
                    indicator()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(settings.scrollbarPadding * 2 + settings.thumbThickness)
                    .run { if (state.activeDraggableModifier.value) then(draggableModifier) else this }
            )
        },
        measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                val placeableThumb = placeables[0]
                val placeableIndicator = placeables[1]
                val placeableScrollbarArea = placeables[2]

                val offset = (constraints.maxWidth.toFloat() * thumbOffsetNormalized).toInt()

                val hideDisplacementPx = when (settings.side) {
                    ScrollbarLayoutSide.Start -> -state.hideDisplacement.value.roundToPx()
                    ScrollbarLayoutSide.End -> +state.hideDisplacement.value.roundToPx()
                }

                placeableThumb.placeRelative(
                    y = when (settings.side) {
                        ScrollbarLayoutSide.Start -> 0
                        ScrollbarLayoutSide.End -> constraints.maxHeight - placeableThumb.height
                    } + hideDisplacementPx,
                    x = offset
                )
                placeableIndicator.placeRelative(
                    y = when (settings.side) {
                        ScrollbarLayoutSide.Start -> 0 + placeableThumb.height
                        ScrollbarLayoutSide.End -> constraints.maxHeight - placeableThumb.height - placeableIndicator.height
                    } + hideDisplacementPx,
                    x = offset + placeableThumb.width / 2 - placeableIndicator.width / 2
                )
                placeableScrollbarArea.placeRelative(
                    y = when (settings.side) {
                        ScrollbarLayoutSide.Start -> 0
                        ScrollbarLayoutSide.End -> constraints.maxHeight - placeableScrollbarArea.height
                    },
                    x = 0
                )
            }
        }
    )
}

