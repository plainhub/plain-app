package com.ismartcoding.plain.ui.page.home

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.compose.LocalPlatformContext
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.platform.combineBitmapGrid
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.TagNameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.BaseMediaViewModel
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Sidebar drawer content for media pages. Shows All, Trash, Folders, and Tags.
 * Inspired by the desktop sidebar layout.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var selectedTagForMenu by remember { mutableStateOf<DTag?>(null) }

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
        NavigationDrawerItem(
            label = { Text(stringResource(Res.string.all)) },
            icon = { Icon(painterResource(Res.drawable.layout_grid), null, modifier = Modifier.size(24.dp)) },
            selected = !mediaVM.trash.value && mediaVM.bucketId.value.isEmpty() && mediaVM.tag.value == null && (mediaVM !is DocsViewModel || (mediaVM as DocsViewModel).fileType.value.isEmpty()),
            onClick = {
                mediaVM.trash.value = false
                mediaVM.bucketId.value = ""
                mediaVM.tag.value = null
                if (mediaVM is DocsViewModel) (mediaVM as DocsViewModel).fileType.value = ""
                loadMedia()
                scope.launch { drawerState.close() }
            },
            badge = { Text(mediaFoldersVM.totalBucket.value?.itemCount?.toString() ?: "0") }
        )

        // Trash
        if (AppFeatureType.MEDIA_TRASH.has()) {
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.trash)) },
                icon = { Icon(painterResource(Res.drawable.trash_2), null, modifier = Modifier.size(24.dp)) },
                selected = mediaVM.trash.value,
                onClick = {
                    mediaVM.trash.value = true
                    mediaVM.bucketId.value = ""
                    mediaVM.tag.value = null
                    if (mediaVM is DocsViewModel) (mediaVM as DocsViewModel).fileType.value = ""
                    loadMedia()
                    scope.launch { drawerState.close() }
                },
                badge = { Text(mediaFoldersVM.trashCount.intValue.toString()) }
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
                        NavigationDrawerItem(
                            label = { Text(tab.title) },
                            icon = { Icon(painterResource(Res.drawable.file_digit), null, modifier = Modifier.size(24.dp)) },
                            selected = !mediaVM.trash.value && docsVM.fileType.value == tab.value,
                            onClick = {
                                mediaVM.trash.value = false
                                mediaVM.bucketId.value = ""
                                mediaVM.tag.value = null
                                docsVM.fileType.value = tab.value
                                loadMedia()
                                scope.launch { drawerState.close() }
                            },
                            badge = { Text(tab.count.toString()) }
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
                    NavigationDrawerItem(
                        label = { Text(bucket.name) },
                        icon = {
                            if (isMedia && bucket.topItems.isNotEmpty()) {
                                val bitmapResult = remember(bucket.id, bucket.topItems) { mutableStateOf<Any?>(null) }
                                LaunchedEffect(bucket.id, bucket.topItems) {
                                    bitmapResult.value = withContext(Dispatchers.Default) {
                                        combineBitmapGrid(bucket.topItems, 100)
                                    }
                                }
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(LocalPlatformContext.current)
                                            .data(bitmapResult.value)
                                            .build()
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(painterResource(Res.drawable.folder), null, modifier = Modifier.size(24.dp))
                            }
                        },
                        selected = !mediaVM.trash.value && mediaVM.bucketId.value == bucket.id,
                        onClick = {
                            mediaVM.trash.value = false
                            mediaVM.bucketId.value = bucket.id
                            mediaVM.tag.value = null
                            if (mediaVM is DocsViewModel) (mediaVM as DocsViewModel).fileType.value = ""
                            loadMedia()
                            scope.launch { drawerState.close() }
                        },
                        badge = { Text(bucket.itemCount.toString()) }
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
                Box {
                    val isSelected = !mediaVM.trash.value && mediaVM.tag.value?.id == tag.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .combinedClickable(
                                onClick = {
                                    mediaVM.trash.value = false
                                    mediaVM.bucketId.value = ""
                                    mediaVM.tag.value = tag
                                    if (mediaVM is DocsViewModel) (mediaVM as DocsViewModel).fileType.value = ""
                                    loadMedia()
                                    scope.launch { drawerState.close() }
                                },
                                onLongClick = {
                                    selectedTagForMenu = tag
                                }
                            ),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.tag),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            HorizontalSpace(12.dp)
                            Text(
                                text = tag.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = tag.count.toString(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (selectedTagForMenu?.id == tag.id) {
                        PDropdownMenu(
                            expanded = true,
                            onDismissRequest = { selectedTagForMenu = null }
                        ) {
                            PDropdownMenuItem(
                                text = { Text(stringResource(Res.string.edit)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.square_pen), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    selectedTagForMenu = null
                                    tagsVM.showEditDialog(tag)
                                }
                            )
                            PDropdownMenuItem(
                                text = { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.red) },
                                leadingIcon = { Icon(painterResource(Res.drawable.trash_2), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.red) },
                                onClick = {
                                    selectedTagForMenu = null
                                    DialogHelper.confirmToDelete {
                                        tagsVM.deleteTag(tag.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    TagNameDialog(tagsVM) {
        tagsVM.loadAsync()
    }
}

@Composable
private fun SidebarSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAction: (() -> Unit)? = null,
    actionIcon: org.jetbrains.compose.resources.DrawableResource? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .clickable { onToggle() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalSpace(dp = 4.dp)
            Icon(
                painter = painterResource(if (isExpanded) Res.drawable.chevron_down else Res.drawable.chevron_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onAction != null && actionIcon != null) {
            PIconButton(
                icon = actionIcon,
                contentDescription = null,
                click = onAction
            )
        }
    }
}
