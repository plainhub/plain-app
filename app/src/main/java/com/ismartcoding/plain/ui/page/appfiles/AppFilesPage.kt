package com.ismartcoding.plain.ui.page.appfiles

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.AppFilesViewModel
import com.ismartcoding.plain.ui.page.appfiles.components.AppFileListContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilesPage(
    navController: NavHostController,
    appFilesVM: AppFilesViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val previewerState = rememberPreviewerState()
    val files by appFilesVM.itemsFlow.collectAsState()
    val isLoading = appFilesVM.isLoading.value

    val refreshState = rememberRefreshLayoutState {
        scope.launch {
            withIO { appFilesVM.loadAsync() }
            setRefreshState(RefreshContentState.Finished)
        }
    }

    LaunchedEffect(Unit) {
        withIO { appFilesVM.loadAsync() }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    NavigationBackIcon { navController.navigateUp() }
                },
                title = stringResource(R.string.app_files),
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
                        previewerState = previewerState,
                        onRefresh = {
                            scope.launch {
                                withIO { appFilesVM.loadAsync() }
                            }
                        },
                    )
                }
            }
        },
    )

    MediaPreviewer(state = previewerState)
}
