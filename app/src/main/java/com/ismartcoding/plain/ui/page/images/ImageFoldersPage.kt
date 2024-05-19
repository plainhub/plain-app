package com.ismartcoding.plain.ui.page.images

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.pinchzoomgrid.PinchZoomGridLayout
import com.ismartcoding.plain.ui.base.pinchzoomgrid.rememberPinchZoomGridState
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.MediaBucketGridItem
import com.ismartcoding.plain.ui.models.ImageFoldersViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImageFoldersPage(
    navController: NavHostController,
    viewModel: ImageFoldersViewModel = viewModel(),
) {
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var lastCellIndex by remember { mutableIntStateOf(4) }
    var canScroll by rememberSaveable { mutableStateOf(true) }

    val pinchState = rememberPinchZoomGridState(
        cellsList = PlainTheme.cellsList,
        initialCellsIndex = lastCellIndex
    )
    LaunchedEffect(pinchState.isZooming) {
        canScroll = !pinchState.isZooming
        lastCellIndex = PlainTheme.cellsList.indexOf(pinchState.currentCells)
    }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync(context) }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync(context)
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(id = R.string.folders))
        },
    ) {
        PullToRefresh(
            refreshLayoutState = topRefreshLayoutState,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (itemsState.isNotEmpty()) {
                    PinchZoomGridLayout(context = context, state = pinchState, scope = scope) {
                        LazyVerticalGrid(
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            columns = gridCells,
                            userScrollEnabled = canScroll,
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            items(itemsState,
                                key = {
                                    it.id
                                },
                                contentType = {
                                    "image"
                                },
                                span = {
                                    GridItemSpan(1)
                                }) { m ->
                                MediaBucketGridItem(
                                    navController = navController,
                                    m = m,
                                )
                            }
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "bottomSpace"
                            ) {
                                BottomSpace()
                            }
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        }
    }
}
