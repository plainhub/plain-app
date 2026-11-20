package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.changes
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
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.components.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorListPage(
    navController: NavHostController,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    vm: ImageEditorViewModel = viewModel { ImageEditorViewModel(com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient(source.api)) },
) {
    val itemsState by vm.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { withIO { vm.loadAsync() }; setRefreshState(RefreshContentState.Finished) }
    }

    LaunchedEffect(source, vm) {
        val updates = when (source) {
            com.ismartcoding.plain.rust.graphql.ContentSource.Local -> AppSubscriptions.subscription<Boolean>("contentChanged")
            is com.ismartcoding.plain.rust.graphql.ContentSource.Remote -> source.client.subscriptionUpdates(listOf(com.ismartcoding.plain.rust.graphql.GraphqlOperation("subscription { contentChanged }")))
        }
        vm.observe(updates)
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
                val failed = vm.loadState.value as? EditorLoadState.Failed
                if (failed != null) {
                    com.ismartcoding.plain.ui.components.NoDataView(message = failed.message, icon = null)
                } else if (itemsState.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                    ) {
                        item { TopSpace() }
                        items(itemsState, key = { it.id }) { project ->
                            ImageEditorProjectListItem(
                                project = project,
                                onClick = { navController.navigate(Routing.ImageEditorDetail(project.id)) },
                                onDelete = {
                                    scope.launch {
                                        confirmActionAsync(
                                            Res.string.delete,
                                            Res.string.confirm_to_delete,
                                            danger = true,
                                            callback = { vm.deleteProject(project.id) },
                                        )
                                    }
                                },
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item { BottomSpace(paddingValues) }
                    }
                } else {
                    NoDataColumn(loading = vm.loadState.value == EditorLoadState.Loading)
                }
            }
        }
    }
}
