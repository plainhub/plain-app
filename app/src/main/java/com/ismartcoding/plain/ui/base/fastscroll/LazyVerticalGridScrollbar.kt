package com.ismartcoding.plain.ui.base.fastscroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.base.fastscroll.controller.rememberLazyGridStateController

@Composable
fun LazyVerticalGridScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    settings: ScrollbarSettings = ScrollbarSettings.Default,
    indicatorContent: (@Composable (index: Int, isThumbSelected: Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (!settings.enabled) content()
    else Box(modifier) {
        content()
        val controller = rememberLazyGridStateController(
            state = state,
            thumbMinLength = settings.thumbMinLength,
            alwaysShowScrollBar = settings.alwaysShowScrollbar,
            selectionMode = settings.selectionMode,
        )

        ElementScrollbar(
            stateController = controller,
            modifier = modifier,
            settings = settings,
            indicatorContent = indicatorContent
        )
    }
}

