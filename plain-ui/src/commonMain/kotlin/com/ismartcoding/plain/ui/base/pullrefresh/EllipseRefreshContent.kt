package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun RefreshLayoutState.EllipseRefreshContent(
    min: Dp = minDp,
    color: Color = Color.Black,
    content: @Composable (BoxScope.(RefreshLayoutState) -> Unit)? = null,
    innerContent: @Composable (BoxScope.(RefreshLayoutState) -> Unit)? = null,
) {
    val isHorizontal =
        remember(getComposePositionState()) { getComposePositionState().value.isHorizontal() }
    val density = LocalDensity.current
    val min_2 = remember(min) { min / 2 }
    val min_4 = remember(min) { min / 4 }
    Box(
        modifier =
            if (isHorizontal) {
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = min_4)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = min_4)
            },
    ) {
        Box(
            modifier =
                Modifier
                    .let {
                        if (isHorizontal) {
                            it
                                .height(min)
                                .width(
                                    maxOf(
                                        density.run { abs(getRefreshContentOffset()).toDp() } - min_2,
                                        min,
                                    ),
                                )
                        } else {
                            it
                                .width(min)
                                .height(
                                    maxOf(
                                        density.run { abs(getRefreshContentOffset()).toDp() } - min_2,
                                        min,
                                    ),
                                )
                        }
                    }
                    .border(
                        border =
                            BorderStroke(
                                width = 2.dp,
                                color = color,
                            ),
                        shape = CircleShape,
                    )
                    .align(Alignment.Center),
        ) {
            innerContent?.invoke(this, this@EllipseRefreshContent)
        }
        content?.invoke(this, this@EllipseRefreshContent)
    }
}

private val minDp = 40.dp
