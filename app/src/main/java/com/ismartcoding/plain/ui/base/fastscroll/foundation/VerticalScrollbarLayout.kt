package com.ismartcoding.plain.ui.base.fastscroll.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarLayoutSide

@Composable
internal fun VerticalScrollbarLayout(
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
                    .height(48.dp)
                    .padding(
                        start = if (settings.side == ScrollbarLayoutSide.Start) settings.scrollbarPadding else 0.dp,
                        end = if (settings.side == ScrollbarLayoutSide.End) settings.scrollbarPadding else 0.dp,
                    )
                    .alpha(state.hideAlpha.value)
                    .clip(
                        RoundedCornerShape(
                            topStartPercent = 100,
                            bottomStartPercent = 100,
                            topEndPercent = 0,
                            bottomEndPercent = 0
                        )
                    )
                    .width(settings.thumbThickness)
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
                    .fillMaxHeight()
                    .width(settings.scrollbarPadding * 2 + settings.thumbThickness)
                    .run { if (state.activeDraggableModifier.value) then(draggableModifier) else this }
            )
        },
        measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                val placeableThumb = placeables[0]
                val placeableIndicator = placeables[1]
                val placeableScrollbarArea = placeables[2]

                val offset = (constraints.maxHeight.toFloat() * thumbOffsetNormalized).toInt()

                val hideDisplacementPx = when (settings.side) {
                    ScrollbarLayoutSide.Start -> -state.hideDisplacement.value.roundToPx()
                    ScrollbarLayoutSide.End -> +state.hideDisplacement.value.roundToPx()
                }

                placeableThumb.placeRelative(
                    x = when (settings.side) {
                        ScrollbarLayoutSide.Start -> 0
                        ScrollbarLayoutSide.End -> constraints.maxWidth - placeableThumb.width
                    } + hideDisplacementPx,
                    y = offset
                )

                placeableIndicator.placeRelative(
                    x = when (settings.side) {
                        ScrollbarLayoutSide.Start -> 0 + placeableThumb.width
                        ScrollbarLayoutSide.End -> constraints.maxWidth - placeableThumb.width - placeableIndicator.width
                    } + hideDisplacementPx,
                    y = offset + placeableThumb.height / 2 - placeableIndicator.height / 2
                )
                placeableScrollbarArea.placeRelative(
                    x = when (settings.side) {
                        ScrollbarLayoutSide.Start -> 0
                        ScrollbarLayoutSide.End -> constraints.maxWidth - placeableScrollbarArea.width
                    },
                    y = 0
                )

            }
        }
    )
}
