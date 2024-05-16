package com.ismartcoding.plain.ui.base.fastscroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.ismartcoding.plain.ui.base.fastscroll.controller.rememberScrollStateController
import com.ismartcoding.plain.ui.base.fastscroll.generic.ElementScrollbar


@Composable
fun ColumnScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
    settings: ScrollbarSettings = ScrollbarSettings.Default,
    indicatorContent: (@Composable (normalizedOffset: Float, isThumbSelected: Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (!settings.enabled) content()
    else BoxWithConstraints(modifier) {
        content()
        InternalColumnScrollbar(
            state = state,
            settings = settings,
            visibleLengthDp = with(LocalDensity.current) { constraints.maxHeight.toDp() },
            indicatorContent = indicatorContent,
        )
    }
}

/**
 * Use this variation if you want to place the scrollbar independently of the list position
 * @param visibleLengthDp Visible length of column view
 */
@Composable
fun InternalColumnScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
    settings: ScrollbarSettings = ScrollbarSettings.Default,
    indicatorContent: (@Composable (normalizedOffset: Float, isThumbSelected: Boolean) -> Unit)? = null,
    visibleLengthDp: Dp,
) {
    val stateController = rememberScrollStateController(
        state = state,
        visibleLengthDp = visibleLengthDp,
        thumbMinLength = settings.thumbMinLength,
        alwaysShowScrollBar = settings.alwaysShowScrollbar,
        selectionMode = settings.selectionMode
    )

    ElementScrollbar(
        orientation = Orientation.Vertical,
        stateController = stateController,
        modifier = modifier,
        settings = settings,
        indicatorContent = indicatorContent,
    )
}
