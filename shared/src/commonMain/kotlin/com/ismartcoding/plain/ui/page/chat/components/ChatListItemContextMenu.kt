package com.ismartcoding.plain.ui.page.chat.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.MessageType
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.copyFileToDir
import com.ismartcoding.plain.platform.saveFileToDownloads
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.components.SaveToSheet
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.nav.navigateChatEditText
import com.ismartcoding.plain.ui.nav.navigateEditShare

@Composable
fun ChatListItemContextMenu(
    navController: NavHostController,
    selectedItem: MutableState<VChat?>,
    m: VChat,
    showContextMenu: MutableState<Boolean>,
    onForward: (VChat) -> Unit,
    onEnterSelectMode: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (Set<String>) -> Unit,
) {
    var showSaveSheet by remember { mutableStateOf(false) }
    PDropdownMenu(
        expanded = showContextMenu.value && selectedItem.value == m,
        onDismissRequest = {
            selectedItem.value = null
            showContextMenu.value = false
        },
    ) {
        PDropdownMenuItem(
            text = { Text(stringResource(Res.string.select)) },
            onClick = {
                onEnterSelectMode()
                onSelect(m.id)
                selectedItem.value = null
                showContextMenu.value = false
            },
        )
        PDropdownMenuItem(
            text = { Text(stringResource(Res.string.forward)) },
            onClick = {
                selectedItem.value = null
                showContextMenu.value = false
                onForward(m)
            },
        )
        if (m.type == MessageType.IMAGES) {
            PDropdownMenuItem(
                text = { Text(stringResource(Res.string.save_as)) },
                onClick = {
                    selectedItem.value = null
                    showContextMenu.value = false
                    showSaveSheet = true
                },
            )
        }
        if (m.value is DMessageText) {
            PDropdownMenuItem(
                text = { Text(stringResource(Res.string.copy_text)) },
                onClick = {
                    selectedItem.value = null
                    showContextMenu.value = false
                    val text = (m.value as DMessageText).text
                    setClipboardText(LocaleHelper.getString(Res.string.message), text)
                    DialogHelper.showTextCopiedMessage(text)
                },
            )
            if (m.fromId == "me") {
                PDropdownMenuItem(
                    text = { Text(stringResource(Res.string.edit_text)) },
                    onClick = {
                        selectedItem.value = null
                        showContextMenu.value = false
                        val content = (m.value as DMessageText).text
                        navController.navigateChatEditText(m.id, content)
                    },
                )
            }
        }
        if (m.type == MessageType.SHARE && m.fromId == "me") {
            PDropdownMenuItem(
                text = { Text(stringResource(Res.string.edit)) },
                onClick = {
                    selectedItem.value = null
                    showContextMenu.value = false
                    navController.navigateEditShare((m.value as DMessageShare).shareId)
                },
            )
        }
        PDropdownMenuItem(
            text = { Text(stringResource(Res.string.delete)) },
            onClick = {
                selectedItem.value = null
                showContextMenu.value = false
                onDelete(setOf(m.id))
            },
        )
    }

    if (showSaveSheet) {
        val images = (m.value as? DMessageImages)?.items.orEmpty()
        SaveToSheet(
            title = images.firstOrNull()?.fileName?.takeIf { it.isNotEmpty() }
                ?: stringResource(Res.string.image),
            onDismiss = { showSaveSheet = false },
            onDownloads = {
                showSaveSheet = false
                coMain {
                    saveMessageImages(images) { path, name -> withIO { saveFileToDownloads(path, name) } }
                }
            },
            onDirectory = { dir ->
                showSaveSheet = false
                coMain {
                    saveMessageImages(images) { path, name -> withIO { copyFileToDir(path, dir, name) } }
                }
            },
        )
    }
}

/** Saves every image of an IMAGES message to [sink]'s destination, then reports the first saved path. */
private suspend fun saveMessageImages(
    images: List<DMessageFile>,
    sink: suspend (path: String, name: String) -> String,
) {
    var firstSavedPath = ""
    images.forEach { image ->
        val path = image.uri.getFinalPath()
        val name = image.fileName.ifEmpty { path.getFilenameFromPath() }
        val result = sink(path, name)
        if (result.isNotEmpty() && firstSavedPath.isEmpty()) firstSavedPath = result
    }
    if (firstSavedPath.isNotEmpty()) {
        DialogHelper.showConfirmDialog("", LocaleHelper.getStringFAsync(Res.string.file_save_to, firstSavedPath))
    } else {
        DialogHelper.showErrorMessage("")
    }
}
