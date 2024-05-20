package com.ismartcoding.plain.ui.page.images

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.MediaBucketGridItem
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaFoldersPage(
    navController: NavHostController,
    dataType: DataType,
    viewModel: MediaFoldersViewModel = viewModel(),
) {
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync(context) }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.dataType.value = dataType
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
                    LazyVerticalGrid(
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(itemsState,
                            key = {
                                it.id
                            },
                            contentType = {
                                "bucket"
                            },
                            span = {
                                GridItemSpan(1)
                            }) { m ->
                            MediaBucketGridItem(
                                navController = navController,
                                m = m,
                                dataType = dataType
                            )
                        }
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "bottomSpace"
                        ) {
                            BottomSpace()
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        }
    }
}
