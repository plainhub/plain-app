package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun rememberRefreshLayoutState(onRefreshListener: RefreshLayoutState.() -> Unit): RefreshLayoutState {
    val listener = rememberUpdatedState(onRefreshListener)
    return remember { RefreshLayoutState { listener.value(this) } }
}
