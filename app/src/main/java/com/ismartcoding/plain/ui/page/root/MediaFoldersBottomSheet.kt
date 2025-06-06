package com.ismartcoding.plain.ui.page.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.ui.base.ActionButtonRefresh
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.components.MediaFolderGridItem
import com.ismartcoding.plain.ui.components.MediaFolderListItem
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.BaseMediaViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun <T : IData> MediaFoldersBottomSheet(
    mediaVM: BaseMediaViewModel<T>,
    mediaFoldersVM: MediaFoldersViewModel,
    tagsVM: TagsViewModel
) {
    if (!mediaVM.showFoldersDialog.value) {
        return
    }

    val itemsState by mediaFoldersVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val showAsList = mediaVM is AudioViewModel

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            mediaFoldersVM.loadAsync(context)
        }
    }

    fun onSelect(id: String) {
        mediaVM.showFoldersDialog.value = false
        mediaVM.bucketId.value = id
        scope.launch(Dispatchers.IO) {
            mediaVM.loadAsync(context, tagsVM)
        }
    }

    PModalBottomSheet(
        onDismissRequest = {
            mediaVM.showFoldersDialog.value = false
        },
        sheetState = sheetState,
    ) {
        Column {
            PBottomSheetTopAppBar(
                titleContent = {
                    Text(
                        text = stringResource(R.string.folders),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    ActionButtonRefresh(
                        loading = mediaFoldersVM.showLoading.value,
                        onClick = {
                            mediaFoldersVM.showLoading.value = true
                            scope.launch {
                                withIO { mediaFoldersVM.loadAsync(context) }
                            }
                        }
                    )
                }
            )
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (itemsState.isNotEmpty()) {
                    if (showAsList) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            item {
                                TopSpace()
                            }
                            if (mediaFoldersVM.totalBucket.value != null) {
                                item {
                                    MediaFolderListItem(
                                        folder = mediaFoldersVM.totalBucket.value!!,
                                        isSelected = mediaVM.bucketId.value.isEmpty(),
                                        onClick = { onSelect("") }
                                    )
                                }
                            }

                            items(
                                items = itemsState,
                                key = { it.id }
                            ) { folder ->
                                MediaFolderListItem(
                                    folder = folder,
                                    isSelected = mediaVM.bucketId.value == folder.id,
                                    onClick = { onSelect(folder.id) }
                                )
                            }
                            item { BottomSpace() }
                        }
                    } else {
                        LazyVerticalGrid(
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (mediaFoldersVM.totalBucket.value != null) {
                                item(
                                    key = "all",
                                    contentType = { "bucket" },
                                    span = { GridItemSpan(1) }
                                ) {
                                    MediaFolderGridItem(
                                        m = mediaFoldersVM.totalBucket.value!!,
                                        isSelected = mediaVM.bucketId.value.isEmpty(),
                                        onClick = { onSelect("") }
                                    )
                                }
                            }
                            items(
                                itemsState,
                                key = { it.id },
                                contentType = { "bucket" },
                                span = { GridItemSpan(1) }
                            ) { m ->
                                MediaFolderGridItem(
                                    m = m,
                                    isSelected = mediaVM.bucketId.value == m.id,
                                    onClick = { onSelect(m.id) }
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
                    NoDataColumn(loading = mediaFoldersVM.showLoading.value)
                }
            }
        }
    }
}
