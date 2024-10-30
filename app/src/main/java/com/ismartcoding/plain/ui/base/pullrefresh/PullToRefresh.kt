package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun PullToRefresh(
    refreshLayoutState: RefreshLayoutState,
    modifier: Modifier = Modifier,
    userEnable: Boolean = true,
    refreshContent: @Composable RefreshLayoutState.() -> Unit = remember {
        { PullToRefreshContent() }
    },
    content: @Composable () -> Unit,
) {
    RefreshLayout(
        refreshContent = refreshContent,
        refreshLayoutState = refreshLayoutState,
        modifier = modifier,
        userEnable = userEnable,
        content = content,
    )
}
