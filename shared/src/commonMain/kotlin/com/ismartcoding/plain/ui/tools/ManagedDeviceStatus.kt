package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import kotlinx.coroutines.launch

@Composable
internal fun ManagedDeviceStatus(loading: Boolean, error: String?, tools: Boolean = false, onRefresh: suspend () -> Boolean) {
    val scope = rememberCoroutineScope()
    val refresh = rememberRefreshLayoutState {
        val state = this
        scope.launch {
            state.setRefreshState(if (onRefresh()) RefreshContentState.Finished else RefreshContentState.Failed)
        }
    }
    PullToRefresh(modifier = Modifier.fillMaxSize(), refreshLayoutState = refresh, userEnable = !loading) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                if (error != null) {
                    ManagedError(error)
                } else if (loading) DeviceLoadingSkeleton(tools) else NoDataColumn(loading = false)
            }
        }
    }
}
