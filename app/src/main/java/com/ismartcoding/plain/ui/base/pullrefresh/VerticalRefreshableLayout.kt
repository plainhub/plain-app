package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun VerticalRefreshableLayout(
    topRefreshLayoutState: RefreshLayoutState,
    bottomRefreshLayoutState: RefreshLayoutState,
    modifier: Modifier = Modifier,
    topRefreshContent: @Composable RefreshLayoutState.() -> Unit =
        remember {
            { PullToRefreshContent() }
        },
    bottomIsLoadFinish: Boolean = false,
    bottomRefreshContent: @Composable RefreshLayoutState.() -> Unit =
        remember(bottomIsLoadFinish) {
            { LoadMoreRefreshContent(bottomIsLoadFinish) }
        },
    topUserEnable: Boolean = true,
    bottomUserEnable: Boolean = true,
    content: @Composable () -> Unit,
) {
    RefreshLayout(
        modifier = modifier,
        refreshContent = topRefreshContent,
        refreshLayoutState = topRefreshLayoutState,
        userEnable = topUserEnable,
    ) {
        RefreshLayout(
            modifier = Modifier.fillMaxSize(),
            refreshContent = bottomRefreshContent,
            refreshLayoutState = bottomRefreshLayoutState,
            composePosition = ComposePosition.Bottom,
            userEnable = bottomUserEnable,
            content = content,
        )
    }
}
