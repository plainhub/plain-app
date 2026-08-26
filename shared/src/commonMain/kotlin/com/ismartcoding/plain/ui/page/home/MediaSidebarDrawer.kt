package com.ismartcoding.plain.ui.page.home

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.MediaSidebarBucketItem
import com.ismartcoding.plain.ui.components.MediaSidebarTagItem
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.components.SidebarSectionHeader
import com.ismartcoding.plain.ui.components.TagNameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.BaseMediaViewModel
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Sidebar drawer content for media pages. Shows All, Trash, Folders, and Tags.
 * Inspired by the desktop sidebar layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : IData> MediaSidebarDrawer(
    mediaVM: BaseMediaViewModel<T>,
    mediaFoldersVM: MediaFoldersViewModel,
    tagsVM: TagsViewModel,
    drawerState: DrawerState,
) {
    val buckets by mediaFoldersVM.itemsFlow.collectAsState()
    val tags by tagsVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var foldersExpanded by remember { mutableStateOf(true) }
    var tagsExpanded by remember { mutableStateOf(true) }
    var extensionsExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            mediaFoldersVM.loadAsync()
            tagsVM.loadAsync()
        }
    }

    fun loadMedia() {
        scope.launch(Dispatchers.Default) {
            mediaVM.loadAsync(tagsVM)
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(NavigationDrawerItemDefaults.ItemPadding)
    ) {
        // Header spacer
        VerticalSpace(dp = 16.dp)

        // All
        SidebarItem(
            label = stringResource(Res.string.all),
            icon = Res.drawable.layout_grid,
            isSelected = !mediaVM.trash.value && mediaVM.bucketId.value.isEmpty() && mediaVM.tag.value == null && (mediaVM !is DocsViewModel || (mediaVM as DocsViewModel).fileType.value.isEmpty()),
            onClick = {
                mediaVM.trash.value = false
                mediaVM.bucketId.value = ""
                mediaVM.tag.value = null
                if (mediaVM is DocsViewModel) (mediaVM as DocsViewModel).fileType.value = ""
                loadMedia()
                scope.launch { drawerState.close() }
            },
            badge = mediaFoldersVM.totalBucket.value?.itemCount?.toString() ?: "0"
        )

        // Trash
        if (AppFeatureType.MEDIA_TRASH.has()) {
            SidebarItem(
                label = stringResource(Res.string.trash),
                icon = Res.drawable.trash_2,
                isSelected = mediaVM.trash.value,
                onClick = {
                    mediaVM.trash.value = true
                    mediaVM.bucketId.value = ""
                    mediaVM.tag.value = null
                    if (mediaVM is DocsViewModel) (mediaVM as DocsViewModel).fileType.value = ""
                    loadMedia()
                    scope.launch { drawerState.close() }
                },
                badge = mediaFoldersVM.trashCount.intValue.toString()
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Extensions Section (Docs only)
        if (mediaVM.dataType == DataType.DOC && mediaVM is DocsViewModel) {
            val docsVM = mediaVM as DocsViewModel
            val extensionTabs = docsVM.tabs.value.filter { it.value != "all" && it.value != "trash" && it.value != "" }
            
            if (extensionTabs.isNotEmpty()) {
                SidebarSectionHeader(
                    title = stringResource(Res.string.type),
                    isExpanded = extensionsExpanded,
                    onToggle = { extensionsExpanded = !extensionsExpanded }
                )

                if (extensionsExpanded) {
                    extensionTabs.forEach { tab ->
                        SidebarItem(
                            label = tab.title,
                            icon = Res.drawable.file_digit,
                            isSelected = !mediaVM.trash.value && docsVM.fileType.value == tab.value,
                            onClick = {
                                mediaVM.trash.value = false
                                mediaVM.bucketId.value = ""
                                mediaVM.tag.value = null
                                docsVM.fileType.value = tab.value
                                loadMedia()
                                scope.launch { drawerState.close() }
                            },
                            badge = tab.count.toString()
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        // Folders Section (hidden when the platform reports no bucket data, e.g. older Android)
        if (buckets.isNotEmpty()) {
            SidebarSectionHeader(
                title = stringResource(Res.string.folders),
                isExpanded = foldersExpanded,
                onToggle = { foldersExpanded = !foldersExpanded },
                onAction = null // No action for folders header yet
            )

            if (foldersExpanded) {
                val isMedia = mediaVM.dataType == DataType.IMAGE || mediaVM.dataType == DataType.VIDEO
                buckets.forEach { bucket ->
                    MediaSidebarBucketItem(
                        m = bucket,
                        isMedia = isMedia,
                        isSelected = !mediaVM.trash.value && mediaVM.bucketId.value == bucket.id,
                        onClick = {
                            mediaVM.trash.value = false
                            mediaVM.bucketId.value = bucket.id
                            mediaVM.tag.value = null
                            if (mediaVM is DocsViewModel) (mediaVM as DocsViewModel).fileType.value = ""
                            loadMedia()
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Tags Section
        SidebarSectionHeader(
            title = stringResource(Res.string.tags),
            isExpanded = tagsExpanded,
            onToggle = { tagsExpanded = !tagsExpanded },
            onAction = {
                tagsVM.showAddDialog()
            },
            actionIcon = Res.drawable.plus
        )

        if (tagsExpanded) {
            tags.forEach { tag ->
                MediaSidebarTagItem(
                    tag = tag,
                    isSelected = !mediaVM.trash.value && mediaVM.tag.value?.id == tag.id,
                    onEdit = { tagsVM.showEditDialog(tag) },
                    onDelete = {
                        DialogHelper.confirmToDelete {
                            tagsVM.deleteTag(tag.id)
                        }
                    },
                    onClick = {
                        mediaVM.trash.value = false
                        mediaVM.bucketId.value = ""
                        mediaVM.tag.value = tag
                        if (mediaVM is DocsViewModel) (mediaVM as DocsViewModel).fileType.value = ""
                        loadMedia()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
        BottomSpace()
    }

    TagNameDialog(tagsVM) {
        tagsVM.loadAsync()
    }
}
