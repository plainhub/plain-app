package com.ismartcoding.plain.ui.page.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.ui.base.ActionButtonRefresh
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
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
                title = stringResource(R.string.folders),
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
                        MediaFolderListContent(
                            listState = listState,
                            totalBucket = mediaFoldersVM.totalBucket.value,
                            items = itemsState,
                            selectedBucketId = mediaVM.bucketId.value,
                            onSelect = ::onSelect,
                        )
                    } else {
                        MediaFolderGridContent(
                            gridState = gridState,
                            totalBucket = mediaFoldersVM.totalBucket.value,
                            items = itemsState,
                            selectedBucketId = mediaVM.bucketId.value,
                            onSelect = ::onSelect,
                        )
                    }
                } else {
                    NoDataColumn(loading = mediaFoldersVM.showLoading.value)
                }
            }
        }
    }
}
