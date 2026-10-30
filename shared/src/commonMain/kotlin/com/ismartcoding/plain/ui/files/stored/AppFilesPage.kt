package com.ismartcoding.plain.ui.files.stored

import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.changes
import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.files.stored.components.AppFileListContent
import com.ismartcoding.plain.ui.state.launchSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilesPage(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    appFilesVM: AppFilesViewModel = viewModel { AppFilesViewModel(source) },
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val previewerState = rememberPreviewerState()
    val files by appFilesVM.itemsFlow.collectAsState()
    val isLoading = appFilesVM.showLoading.value
    val noMore = appFilesVM.noMore.value

    val refreshState = rememberRefreshLayoutState {
        scope.launchSafe {
            appFilesVM.loadAsync()
            setRefreshState(RefreshContentState.Finished)
        }
    }

    LaunchedEffect(appFilesVM) {
        launchSafe { appFilesVM.loadAsync() }
        AppSubscriptions.contentChanged.changes().collect {
            launchSafe { appFilesVM.loadAsync() }.join()
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    NavigationBackIcon { navController.navigateUp() }
                },
                title = stringResource(Res.string.app_files),
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                PullToRefresh(
                    modifier = Modifier.fillMaxSize(),
                    refreshLayoutState = refreshState,
                ) {
                    AppFileListContent(
                        navController = navController,
                        files = files,
                        isLoading = isLoading,
                        noMore = noMore,
                        previewerState = previewerState,
                        audioQueueVM = audioQueueVM,
                        onRefresh = {
                            scope.launchSafe {
                                appFilesVM.loadAsync()
                            }
                        },
                        onLoadMore = {
                            scope.launchSafe {
                                appFilesVM.moreAsync()
                            }
                        },
                    )
                }
            }
        },
    )

    MediaPreviewer(state = previewerState)
}
