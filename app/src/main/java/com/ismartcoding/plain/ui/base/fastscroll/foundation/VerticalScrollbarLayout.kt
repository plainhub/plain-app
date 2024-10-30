package com.ismartcoding.plain.ui.base.fastscroll.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R

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

    Layout(
        content = {
            Box(
                modifier = Modifier
                    .alpha(state.hideAlpha.value)
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(
                            topStartPercent = 100,
                            bottomStartPercent = 100
                        )
                    )
                    .padding(vertical = 2.dp)
                    .run { if (state.activeDraggableModifier.value) then(draggableModifier) else this },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.scroll_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

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
                    .width(16.dp)
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

                val hideDisplacementPx = state.hideDisplacement.value.roundToPx()

                placeableThumb.placeRelative(
                    x = constraints.maxWidth - placeableThumb.width + hideDisplacementPx + 8.dp.roundToPx(),
                    y = offset
                )

                placeableIndicator.placeRelative(
                    x = constraints.maxWidth - placeableThumb.width - placeableIndicator.width + hideDisplacementPx,
                    y = offset + placeableThumb.height / 2 - placeableIndicator.height / 2
                )
                placeableScrollbarArea.placeRelative(
                    x = constraints.maxWidth - placeableScrollbarArea.width,
                    y = 0
                )

            }
        }
    )
}
