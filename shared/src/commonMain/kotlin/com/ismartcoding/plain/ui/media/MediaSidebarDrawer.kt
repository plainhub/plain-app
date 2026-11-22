package com.ismartcoding.plain.ui.media

import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.DisposableEffect
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
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.MediaSidebarBucketItem
import com.ismartcoding.plain.ui.components.MediaSidebarTagItem
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.components.SidebarSectionHeader
import com.ismartcoding.plain.ui.components.TagNameDialog
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.files.documents.DocsViewModel
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.file_digit as ui_drawable_file_digit
import com.ismartcoding.plain.ui.resources.layout_grid as ui_drawable_layout_grid
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus
import com.ismartcoding.plain.ui.resources.trash_2 as ui_drawable_trash_2

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

    var foldersExpanded by remember { mutableStateOf(true) }
    var tagsExpanded by remember { mutableStateOf(true) }
    var extensionsExpanded by remember { mutableStateOf(true) }

    // Drawer data loads on first open, not on page entry: the drawer stays
    // composed (hidden) inside ModalNavigationDrawer, so an eager load would
    // land mid enter-transition and recompose its rows while the page is
    // still animating up. Pages still preload folders/tags via their own
    // post-transition reload, so this is only a fallback.
    val openLoader = remember { OnceOnOpenLoader() }
    LaunchedEffect(drawerState.targetValue) {
        if (openLoader.shouldLoad(drawerState.targetValue == DrawerValue.Open)) {
            scope.launchSafe {
                if (mediaFoldersVM.coverRevision == 0) mediaFoldersVM.loadAsync()
                tagsVM.loadAsync()
            }
        }
    }
    DisposableEffect(mediaVM, mediaFoldersVM) {
        val refreshFolders: suspend () -> Unit = { mediaFoldersVM.loadAsync() }
        mediaVM.onMediaChanged = refreshFolders
        onDispose {
            if (mediaVM.onMediaChanged === refreshFolders) mediaVM.onMediaChanged = null
        }
    }

    fun loadMedia() {
        scope.launchSafe(onDone = { mediaVM.showLoading.value = false }) {
            mediaVM.loadAsync(tagsVM)
        }
    }

    LazyColumn(contentPadding = NavigationDrawerItemDefaults.ItemPadding) {
        item { VerticalSpace(dp = 16.dp) }
        item {
            SidebarItem(
                label = stringResource(Res.string.all),
                icon = UiRes.drawable.ui_drawable_layout_grid,
                isSelected = !mediaVM.trash.value && mediaVM.bucketId.value.isEmpty() && mediaVM.tag.value == null && (mediaVM !is DocsViewModel || mediaVM.fileType.value.isEmpty()),
                onClick = {
                    mediaVM.trash.value = false
                    mediaVM.bucketId.value = ""
                    mediaVM.tag.value = null
                    if (mediaVM is DocsViewModel) mediaVM.fileType.value = ""
                    loadMedia()
                    scope.launch { drawerState.close() }
                },
                badge = mediaFoldersVM.totalBucket.value?.itemCount?.toString() ?: "0"
            )
        }
        if (Capability.MEDIA_TRASH in getDeviceCapabilities()) {
            item {
                SidebarItem(
                    label = stringResource(Res.string.trash),
                    icon = UiRes.drawable.ui_drawable_trash_2,
                    isSelected = mediaVM.trash.value,
                    onClick = {
                        mediaVM.trash.value = true
                        mediaVM.bucketId.value = ""
                        mediaVM.tag.value = null
                        if (mediaVM is DocsViewModel) mediaVM.fileType.value = ""
                        loadMedia()
                        scope.launch { drawerState.close() }
                    },
                    badge = mediaFoldersVM.trashCount.intValue.toString()
                )
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        if (mediaVM is DocsViewModel) {
            val tabs = mediaVM.tabs.value.filter { it.value != "all" && it.value != "trash" && it.value != "" }
            if (tabs.isNotEmpty()) {
                item {
                    SidebarSectionHeader(
                        title = stringResource(Res.string.type),
                        isExpanded = extensionsExpanded,
                        onToggle = { extensionsExpanded = !extensionsExpanded }
                    )
                }
                if (extensionsExpanded) {
                    items(tabs, key = { "extension:${it.value}" }) { tab ->
                        SidebarItem(
                            label = tab.title,
                            icon = UiRes.drawable.ui_drawable_file_digit,
                            isSelected = !mediaVM.trash.value && mediaVM.fileType.value == tab.value,
                            onClick = {
                                mediaVM.trash.value = false
                                mediaVM.bucketId.value = ""
                                mediaVM.tag.value = null
                                mediaVM.fileType.value = tab.value
                                loadMedia()
                                scope.launch { drawerState.close() }
                            },
                            badge = tab.count.toString()
                        )
                    }
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }
        }

        if (buckets.isNotEmpty()) {
            item {
                SidebarSectionHeader(
                    title = stringResource(Res.string.folders),
                    isExpanded = foldersExpanded,
                    onToggle = { foldersExpanded = !foldersExpanded },
                    onAction = null
                )
            }
            if (foldersExpanded) {
                val isMedia = mediaVM.dataType == DataType.IMAGE || mediaVM.dataType == DataType.VIDEO
                val coversEnabled = openLoader.hasOpened
                val coverRevision = mediaFoldersVM.coverRevision
                items(buckets, key = { "bucket:${it.id}" }) { bucket ->
                    MediaSidebarBucketItem(
                        m = bucket,
                        isMedia = isMedia,
                        coversEnabled = coversEnabled,
                        coverRevision = coverRevision,
                        isSelected = !mediaVM.trash.value && mediaVM.bucketId.value == bucket.id,
                        onClick = {
                            mediaVM.trash.value = false
                            mediaVM.bucketId.value = bucket.id
                            mediaVM.tag.value = null
                            if (mediaVM is DocsViewModel) mediaVM.fileType.value = ""
                            loadMedia()
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        item {
            SidebarSectionHeader(
                title = stringResource(Res.string.tags),
                isExpanded = tagsExpanded,
                onToggle = { tagsExpanded = !tagsExpanded },
                onAction = {
                    tagsVM.showAddDialog()
                },
                actionIcon = UiRes.drawable.ui_drawable_plus
            )
        }
        if (tagsExpanded) {
            items(tags, key = { "tag:${it.id}" }) { tag ->
                MediaSidebarTagItem(
                    tag = tag,
                    isSelected = !mediaVM.trash.value && mediaVM.tag.value?.id == tag.id,
                    onEdit = { tagsVM.showEditDialog(tag) },
                    onDelete = {
                        scope.launch {
                            confirmActionAsync(
                                Res.string.delete,
                                Res.string.confirm_to_delete,
                                callback = {
                                    tagsVM.deleteTag(tag.id)
                                },
                                danger = true
                            )
                        }
                    },
                    onClick = {
                        mediaVM.trash.value = false
                        mediaVM.bucketId.value = ""
                        mediaVM.tag.value = tag
                        if (mediaVM is DocsViewModel) mediaVM.fileType.value = ""
                        loadMedia()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
        item { BottomSpace() }
    }

    TagNameDialog(tagsVM) {
        tagsVM.loadAsync()
    }
}

class OnceOnOpenLoader {
    var hasOpened by mutableStateOf(false)
        private set

    fun shouldLoad(isOpen: Boolean): Boolean {
        if (!isOpen || hasOpened) return false
        hasOpened = true
        return true
    }
}
