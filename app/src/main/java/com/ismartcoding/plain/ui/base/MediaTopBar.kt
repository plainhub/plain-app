package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.SortAndBrowseDialog
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.BaseMediaViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun <T : IData> MediaTopBar(
    navController: NavHostController,
    mediaVM: BaseMediaViewModel<T>,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
    dragSelectState: DragSelectState,
    scrollBehavior: TopAppBarScrollBehavior,
    bucketsMap: Map<String, DMediaBucket>,
    itemsState: List<T>,
    scrollToTop: () -> Unit,
    defaultNavigationIcon: (@Composable () -> Unit)? = null,
    onSortSelected: (context: android.content.Context, sortBy: FileSortBy) -> Unit = { _, _ -> },
    onSearchAction: (context: android.content.Context, tagsViewModel: TagsViewModel) -> Unit
) {
    val context = LocalContext.current
    val isDocs = mediaVM.dataType == DataType.DOC

    val title = getMediaPageTitle(mediaVM.dataType, castVM, bucket = bucketsMap[mediaVM.bucketId.value], dragSelectState, mediaVM.tag, mediaVM.trash)
    val containerColor = if (castVM.castMode.value) MaterialTheme.colorScheme.secondaryContainer else null

    SearchableTopBar(
        navController = navController,
        viewModel = mediaVM,
        scrollBehavior = scrollBehavior,
        title = title,
        containerColor = containerColor,
        scrollToTop = scrollToTop,
        navigationIcon = {
            if (dragSelectState.selectMode) {
                NavigationCloseIcon {
                    dragSelectState.exitSelectMode()
                }
            } else if (castVM.castMode.value) {
                NavigationCloseIcon {
                    castVM.exitCastMode()
                }
            } else {
                defaultNavigationIcon?.invoke()
            }
        },
        actions = {
            if (!mediaVM.hasPermission.value) {
                return@SearchableTopBar
            }
            if (castVM.castMode.value) {
                return@SearchableTopBar
            }
            if (dragSelectState.selectMode) {
                PTopRightButton(
                    label = stringResource(if (dragSelectState.isAllSelected(itemsState)) R.string.unselect_all else R.string.select_all),
                    click = {
                        dragSelectState.toggleSelectAll(itemsState)
                    },
                )
                HorizontalSpace(dp = 8.dp)
            } else {
                ActionButtonSearch {
                    mediaVM.enterSearchMode()
                }
                if (isQPlus()) {
                    ActionButtonFolders {
                        mediaVM.showFoldersDialog.value = true
                    }
                }
                if (!isDocs) {
                    ActionButtonCast {
                        castVM.showCastDialog.value = true
                    }
                }

                ActionButtonTags {
                    mediaVM.showTagsDialog.value = true
                }

                PIconButton(
                    icon = R.drawable.sort,
                    contentDescription = stringResource(R.string.sort),
                    tint = MaterialTheme.colorScheme.onSurface,
                    click = { mediaVM.showSortAndBrowseDialog.value = true },
                )
            }
        },
        onSearchAction = {
            mediaVM.showLoading.value = true
            onSearchAction(context, tagsVM)
        }
    )

    if (mediaVM.showSortAndBrowseDialog.value) {
        SortAndBrowseDialog(
            context = context,
            mediaVM = mediaVM,
            tagsVM = tagsVM,
            onSortSelected = { sortBy -> onSortSelected(context, sortBy) },
            onDismiss = { mediaVM.showSortAndBrowseDialog.value = false },
        )
    }
}