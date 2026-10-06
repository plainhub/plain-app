package com.ismartcoding.plain.ui.base.pullrefresh

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun RefreshLayoutState.setRefreshState(state: RefreshContentState) {
    when (state) {
        RefreshContentState.Failed -> {
            if (refreshContentState.value == RefreshContentState.Failed)
                return
            if (!isScopeInitialized)
                throw IllegalStateException("[RefreshLayoutState]还未初始化完成,请在[LaunchedEffect]中或composable至少组合一次后使用此方法")
            coroutineScope.launch {
                refreshContentState.value = RefreshContentState.Failed
                delay(300)
                refreshContentOffsetState.animateTo(0f)
            }
        }
        RefreshContentState.Finished -> {
            if (refreshContentState.value == RefreshContentState.Finished)
                return
            if (!isScopeInitialized)
                throw IllegalStateException("[RefreshLayoutState]还未初始化完成,请在[LaunchedEffect]中或composable至少组合一次后使用此方法")
            coroutineScope.launch {
                refreshContentState.value = RefreshContentState.Finished
                markRefreshed()
                delay(300)
                refreshContentOffsetState.animateTo(0f)
            }
        }

        RefreshContentState.Refreshing -> {
            if (refreshContentState.value == RefreshContentState.Refreshing)
                return
            if (!isScopeInitialized)
                throw IllegalStateException("[RefreshLayoutState]还未初始化完成,请在[LaunchedEffect]中或composable至少组合一次后使用此方法")
            coroutineScope.launch {
                refreshContentState.value = RefreshContentState.Refreshing
                runRefreshListener()
                // 兜底路径已经自己收尾了，不能再把刷新头顶回去，见 parkHeaderIfStillRefreshing。
                parkHeaderIfStillRefreshing()
            }
        }

        RefreshContentState.Dragging -> throw IllegalStateException("设置为[RefreshContentState.Dragging]无意义")
    }
}
