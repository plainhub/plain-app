package com.ismartcoding.plain.ui.base.fastscroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.base.fastscroll.controller.rememberLazyListStateController

@Composable
fun LazyColumnScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    settings: ScrollbarSettings = ScrollbarSettings.Default,
    indicatorContent: (@Composable (index: Int, isThumbSelected: Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        content()
        if (settings.enabled) {
            val controller = rememberLazyListStateController(
                state = state,
                thumbMinLength = settings.thumbMinLength,
                alwaysShowScrollBar = settings.alwaysShowScrollbar,
                selectionMode = settings.selectionMode
            )

            ElementScrollbar(
                stateController = controller,
                modifier = Modifier.matchParentSize(),
                settings = settings,
                indicatorContent = indicatorContent,
            )
        }
    }
}
