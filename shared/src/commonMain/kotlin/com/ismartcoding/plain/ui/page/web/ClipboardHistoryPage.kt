package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.models.ClipboardHistoryViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.delete_forever as ui_drawable_delete_forever

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClipboardHistoryPage(
    navController: NavHostController,
    vm: ClipboardHistoryViewModel = viewModel { ClipboardHistoryViewModel() }
) {
    val scope = rememberCoroutineScope()
    val items by vm.itemsFlow.collectAsState()

    LaunchedEffect(vm) {
        launch { vm.loadAsync() }
        Channel.sharedFlow.collect { event ->
            if (event is WebSocketEvent && event.type == EventType.CONTENT_CHANGED) vm.loadAsync()
        }
    }

    val refreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            vm.loadAsync()
            setRefreshState(RefreshContentState.Finished)
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
                title = stringResource(Res.string.clipboard_history),
                actions = {
                    if (items.isNotEmpty()) {
                        ActionButtonMoreWithMenu { dismiss ->
                            PDropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(UiRes.drawable.ui_drawable_delete_forever),
                                        tint = MaterialTheme.colorScheme.error,
                                        contentDescription = stringResource(Res.string.clear_all)
                                    )
                                },
                                onClick = { dismiss(); scope.launch { vm.clearAllAsync() } },
                                text = { Text(text = stringResource(Res.string.clear_all)) }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefresh(
            userEnable = true,
            refreshLayoutState = refreshLayoutState,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
        ) {
            if (items.isEmpty() && !vm.isLoading.value) {
                NoDataColumn(loading = false)
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                ) {
                    item { TopSpace() }
                    items(items, key = { it.id }) { entry ->
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN, vertical = 4.dp)) {
                        ClipboardHistoryListItem(
                            entry = entry,
                            sourceName = vm.sourceNameFor(entry),
                            onCopy = {
                                setClipboardText(
                                    LocaleHelper.getString(Res.string.clipboard_history),
                                    entry.text,
                                )
                            },
                            onDelete = { scope.launch { vm.deleteAsync(entry.id) } }
                        )
                    }
                    }
                    item {
                        if (items.isNotEmpty() && !vm.noMore.value) {
                            LaunchedEffect(items.size) { scope.launch { vm.moreAsync() } }
                        }
                        LoadMoreRefreshContent(vm.noMore.value)
                    }
                    item { BottomSpace(paddingValues) }
                }
            }
        }
    }
}
