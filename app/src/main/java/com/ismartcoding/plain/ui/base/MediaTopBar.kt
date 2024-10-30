package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
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
    showCellsPerRowDialog: Boolean = true,
    onCellsPerRowClick: (() -> Unit)? = null,
    onSearchAction: (context: android.content.Context, tagsViewModel: TagsViewModel) -> Unit
) {
    val context = LocalContext.current

    val title = getMediaPageTitle(mediaVM.dataType, castVM, bucket = bucketsMap[mediaVM.bucketId.value], dragSelectState, mediaVM.tag, mediaVM.trash)

    SearchableTopBar(
        navController = navController,
        viewModel = mediaVM,
        scrollBehavior = scrollBehavior,
        title = title,
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
                ActionButtonFolders {
                    mediaVM.showFoldersDialog.value = true
                }

                ActionButtonCast {
                    castVM.showCastDialog.value = true
                }

                ActionButtonMoreWithMenu { dismiss ->
                    PDropdownMenuItemSort(onClick = {
                        dismiss()
                        mediaVM.showSortDialog.value = true
                    })
                    PDropdownMenuItemTags(onClick = {
                        dismiss()
                        mediaVM.showTagsDialog.value = true
                    })
                    if (showCellsPerRowDialog && onCellsPerRowClick != null) {
                        PDropdownMenuItemCellsPerRow(onClick = {
                            dismiss()
                            onCellsPerRowClick()
                        })
                    }
                }
            }
        },
        onSearchAction = {
            mediaVM.showLoading.value = true
            onSearchAction(context, tagsVM)
        }
    )
}

@Composable
private fun getMediaPageTitle(
    mediaType: DataType,
    castVM: CastViewModel,
    bucket: DMediaBucket?,
    dragSelectState: DragSelectState,
    tag: MutableState<com.ismartcoding.plain.db.DTag?>,
    trash: MutableState<Boolean>
): String {
    val resourceId = when (mediaType) {
        DataType.IMAGE -> R.string.images
        DataType.VIDEO -> R.string.videos
        DataType.AUDIO -> R.string.audios
        else -> R.string.files
    }

    val mediaName = bucket?.name ?: stringResource(id = resourceId)
    return if (castVM.castMode.value) {
        stringResource(id = R.string.cast_mode) + " - " + CastPlayer.currentDevice?.description?.device?.friendlyName
    } else if (dragSelectState.selectMode) {
        LocaleHelper.getStringF(R.string.x_selected, "count", dragSelectState.selectedIds.size)
    } else if (tag.value != null) {
        mediaName + " - " + tag.value!!.name
    } else if (trash.value) {
        stringResource(id = resourceId) + " - " + stringResource(id = R.string.trash)
    } else {
        mediaName
    }
} 