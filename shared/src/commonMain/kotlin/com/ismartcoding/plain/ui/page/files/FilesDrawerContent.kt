package com.ismartcoding.plain.ui.page.files

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.events.FolderKanbanSelectEvent
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PDropdownMenuItemDelete
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.components.SidebarSectionHeader
import com.ismartcoding.plain.ui.components.buildFolderOptions
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.FolderOption
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.shares.expiryLabel
import kotlinx.coroutines.launch

/**
 * Drawer content for the Files page: storage locations as SidebarItems, a
 * collapsible Favorites section (long-press to rename / remove), and a
 * collapsible Shared links section listing each share with its expiry
 * (tap or long-press edit / delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesDrawerContent(
    filesVM: FilesViewModel,
    drawerState: DrawerState,
    navController: NavHostController,
    onSelect: (FolderOption) -> Unit,
) {
    val recentsText = stringResource(Res.string.recents)
    val internalStorageText = stringResource(Res.string.internal_storage)
    val sdcardText = stringResource(Res.string.sdcard)
    val usbStorageText = stringResource(Res.string.usb_storage)
    val fileTransferAssistantText = stringResource(Res.string.app_data)
    val scope = rememberCoroutineScope()
    val options = remember { mutableStateListOf<FolderOption>() }
    var shares by remember { mutableStateOf<List<DShare>>(emptyList()) }
    var contextMenuPath by remember { mutableStateOf<String?>(null) }
    var contextMenuShareId by remember { mutableStateOf<String?>(null) }
    var renameFavorite by remember { mutableStateOf<FolderOption?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var favoritesExpanded by remember { mutableStateOf(true) }
    var sharesExpanded by remember { mutableStateOf(true) }

    val removeFavorite: (String) -> Unit = { fullPath ->
        scope.launch {
            FavoriteFoldersPreference.removeAsync(fullPath)
            filesVM.favoriteFoldersVersion.value++
        }
    }

    LaunchedEffect(filesVM.favoriteFoldersVersion.intValue, filesVM.selectedPathVersion.intValue) {
        val items = buildFolderOptions(filesVM, recentsText, internalStorageText, sdcardText, usbStorageText, fileTransferAssistantText)
        options.clear()
        options.addAll(items)
    }

    // Reload shares whenever the drawer opens or the share list changes
    // (created via dialog, edited on the EditShare page, or deleted here).
    LaunchedEffect(filesVM.sharesVersion.intValue, drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) {
            shares = ShareManager.listShares()
        }
    }

    val favorites = options.filter { it.isFavoriteFolder }

    Column(
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .verticalScroll(rememberScrollState())
    ) {
        VerticalSpace(dp = 16.dp)
        options.filterNot { it.isFavoriteFolder }.forEach { item ->
            SidebarItem(
                label = item.title,
                icon = if (item.type == FilesType.RECENTS) Res.drawable.history else Res.drawable.folder,
                isSelected = item.isChecked,
                onClick = { sendEvent(FolderKanbanSelectEvent(item)); onSelect(item) }
            )
        }

        if (favorites.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SidebarSectionHeader(
                title = stringResource(Res.string.favorites),
                isExpanded = favoritesExpanded,
                onToggle = { favoritesExpanded = !favoritesExpanded },
            )
            if (favoritesExpanded) {
                favorites.forEach { item ->
                    Box {
                        SidebarItem(
                            label = item.title,
                            icon = Res.drawable.folder,
                            isSelected = item.isChecked,
                            onLongClick = { contextMenuPath = item.fullPath },
                            onClick = { sendEvent(FolderKanbanSelectEvent(item)); onSelect(item) }
                        )
                        PDropdownMenu(
                            expanded = contextMenuPath == item.fullPath,
                            onDismissRequest = { contextMenuPath = null },
                        ) {
                            PDropdownMenuItem(
                                text = { Text(stringResource(Res.string.rename)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.pen), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    contextMenuPath = null
                                    renameValue = item.title
                                    renameFavorite = item
                                },
                            )
                            PDropdownMenuItemDelete {
                                contextMenuPath = null
                                removeFavorite(item.fullPath)
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SidebarSectionHeader(
            title = stringResource(Res.string.share_links),
            isExpanded = sharesExpanded,
            onToggle = { sharesExpanded = !sharesExpanded },
        )
        if (sharesExpanded) {
            shares.forEach { share ->
                Box {
                    SidebarItem(
                        label = share.name.ifBlank { share.id },
                        badge = share.expiryLabel(),
                        icon = Res.drawable.link,
                        onClick = { navController.navigate(Routing.EditShare(share.id)) },
                        onLongClick = { contextMenuShareId = share.id },
                    )
                    PDropdownMenu(
                        expanded = contextMenuShareId == share.id,
                        onDismissRequest = { contextMenuShareId = null },
                    ) {
                        PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.edit)) },
                            leadingIcon = { Icon(painterResource(Res.drawable.square_pen), null, modifier = Modifier.size(20.dp)) },
                            onClick = {
                                contextMenuShareId = null
                                navController.navigate(Routing.EditShare(share.id))
                            },
                        )
                        PDropdownMenuItemDelete {
                            contextMenuShareId = null
                            DialogHelper.confirmToDelete {
                                scope.launch {
                                    ShareManager.deleteShare(share.id)
                                    filesVM.sharesVersion.value++
                                }
                            }
                        }
                    }
                }
            }
        }
        VerticalSpace(dp = 16.dp)
    }

    renameFavorite?.let { favorite ->
        TextFieldDialog(
            title = stringResource(Res.string.rename),
            value = renameValue,
            placeholder = stringResource(Res.string.name),
            onValueChange = { renameValue = it },
            onDismissRequest = { renameFavorite = null },
            onConfirm = { name ->
                scope.launch {
                    FavoriteFoldersPreference.renameAsync(favorite.fullPath, name)
                    filesVM.favoriteFoldersVersion.value++
                }
                renameFavorite = null
            },
        )
    }
}
