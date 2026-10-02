package com.ismartcoding.plain.ui.page.imageeditor

import com.ismartcoding.plain.i18n.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.models.ImageEditorViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorListPage(
    navController: NavHostController,
    vm: ImageEditorViewModel = viewModel { ImageEditorViewModel() },
) {
    val itemsState by vm.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { withIO { vm.loadAsync() }; setRefreshState(RefreshContentState.Finished) }
    }

    LaunchedEffect(vm) {
        launch { vm.loadAsync() }
        com.ismartcoding.plain.lib.Channel.sharedFlow.collect { event ->
            if (event is com.ismartcoding.plain.events.WebSocketEvent &&
                event.type == com.ismartcoding.plain.events.EventType.CONTENT_CHANGED) vm.loadAsync()
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.image_editor),
                scrollBehavior = scrollBehavior,
                actions = {
                    PCapsuleMoreClose(
                        onClose = { navController.navigateUp() },
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routing.ImageEditorDetail("")) }) {
                Icon(painter = painterResource(UiRes.drawable.ui_drawable_plus), contentDescription = stringResource(Res.string.add))
            }
        },
    ) { paddingValues ->
        PullToRefresh(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            refreshLayoutState = topRefreshLayoutState,
        ) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                if (itemsState.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                    ) {
                        item { TopSpace() }
                        items(itemsState, key = { it.id.value }) { project ->
                            ImageEditorProjectListItem(
                                project = project,
                                onClick = { navController.navigate(Routing.ImageEditorDetail(project.id.value)) },
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item { BottomSpace(paddingValues) }
                    }
                } else {
                    NoDataColumn(loading = vm.showLoading.value)
                }
            }
        }
    }
}
