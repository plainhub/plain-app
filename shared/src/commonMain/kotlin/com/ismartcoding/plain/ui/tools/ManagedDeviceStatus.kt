package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.srl_header_pulling
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ManagedDeviceStatus(loading: Boolean, error: String?, onRefresh: suspend () -> Boolean) {
    val scope = rememberCoroutineScope()
    val refresh = rememberRefreshLayoutState {
        val state = this
        scope.launch {
            state.setRefreshState(if (onRefresh()) RefreshContentState.Finished else RefreshContentState.Failed)
        }
    }
    PullToRefresh(modifier = Modifier.fillMaxSize(), refreshLayoutState = refresh, userEnable = !loading) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                if (error != null) {
                    ManagedError(error)
                    Text(
                        stringResource(Res.string.srl_header_pulling),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                } else NoDataColumn(loading = loading)
            }
        }
    }
}
