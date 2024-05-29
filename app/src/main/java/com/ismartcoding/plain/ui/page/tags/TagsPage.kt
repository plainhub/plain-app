package com.ismartcoding.plain.ui.page.tags

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.ui.base.DragAnchors
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDraggableElement
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwipeBox
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.SwipeActionButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.TagNameDialog
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TagsPage(
    navController: NavHostController,
    dataType: DataType,
    viewModel: TagsViewModel = viewModel(),
) {
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO {
                    viewModel.loadAsync()
                }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.dataType.value = dataType
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync()
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(R.string.tags),
            )
        },
        floatingActionButton =
        {
            PDraggableElement {
                FloatingActionButton(
                    onClick = {
                        viewModel.showAddDialog()
                    },
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        stringResource(R.string.add),
                    )
                }
            }
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
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                    ) {
                        item {
                            TopSpace()
                        }
                        items(itemsState) { m ->
                            PSwipeBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                endContent = { state ->
                                    SwipeActionButton(
                                        text = stringResource(R.string.delete),
                                        color = colorResource(id = R.color.red),
                                        onClick = {
                                            scope.launch {
                                                state.animateTo(DragAnchors.Center)
                                            }
                                            viewModel.deleteTag(m.id)
                                        })
                                    HorizontalSpace(dp = 32.dp)
                                }
                            ) {
                                PListItem(
                                    modifier = PlainTheme.getCardModifier().clickable {
                                        viewModel.showEditDialog(m)
                                    },
                                    title = m.name,
                                )
                            }
                            VerticalSpace(dp = 8.dp)
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        }
    }

    TagNameDialog(viewModel)
}
