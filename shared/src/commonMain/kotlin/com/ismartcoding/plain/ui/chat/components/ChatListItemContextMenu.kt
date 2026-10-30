package com.ismartcoding.plain.ui.chat.components

import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.features.files.FilesClient

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.chat.models.MessageImages
import com.ismartcoding.plain.features.chat.models.MessageShare
import com.ismartcoding.plain.features.chat.models.MessageText
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.ui.extensions.getFinalPath
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.platform.files.saveFileToDownloads
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.components.SaveToSheet
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.chat.VChat
import com.ismartcoding.plain.ui.navigation.navigateChatEditText
import com.ismartcoding.plain.ui.navigation.navigateEditShare
import org.jetbrains.compose.resources.stringResource

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
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    var showSaveSheet by remember { mutableStateOf(false) }
    PDropdownMenu(
        expanded = showContextMenu.value && selectedItem.value == m,
        onDismissRequest = {
            selectedItem.value = null
            showContextMenu.value = false
        },
    ) {
        if (!m.id.startsWith("pending_")) {
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
        }
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
        if (m.value is MessageText) {
            PDropdownMenuItem(
                text = { Text(stringResource(Res.string.copy_text)) },
                onClick = {
                    selectedItem.value = null
                    showContextMenu.value = false
                    val text = (m.value as MessageText).text
                    setClipboardText(LocaleHelper.getString(Res.string.message), text)
                },
            )
            if (m.fromId == "me" && !m.id.startsWith("pending_")) {
                PDropdownMenuItem(
                    text = { Text(stringResource(Res.string.edit_text)) },
                    onClick = {
                        selectedItem.value = null
                        showContextMenu.value = false
                        val content = (m.value as MessageText).text
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
                    navController.navigateEditShare((m.value as MessageShare).shareId)
                },
            )
        }
        if (!m.id.startsWith("pending_") || m.status != ChatStatus.PENDING) PDropdownMenuItem(
            text = { Text(stringResource(Res.string.delete)) },
            onClick = {
                selectedItem.value = null
                showContextMenu.value = false
                onDelete(setOf(m.id))
            },
        )
    }

    if (showSaveSheet) {
        val images = (m.value as? MessageImages)?.items.orEmpty()
        SaveToSheet(
            source = source,
            title = images.firstOrNull()?.fileName?.takeIf { it.isNotEmpty() }
                ?: stringResource(Res.string.image),
            onDismiss = { showSaveSheet = false },
            onDownloads = {
                showSaveSheet = false
                coMain {
                    saveMessageImages(images, source) { path, name -> withIO { if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) source.files.copyToDirectory(path, source.info.downloadsDir, name) else saveFileToDownloads(path, name) } }
                }
            },
            onDirectory = { dir ->
                showSaveSheet = false
                coMain {
                    saveMessageImages(images, source) { path, name -> withIO { source.files.copyToDirectory(path, dir, name) } }
                }
            },
        )
    }
}

/** Saves every image of an IMAGES message to [sink]'s destination, then reports the first saved path. */
private suspend fun saveMessageImages(
    images: List<MessageFile>,
    source: com.ismartcoding.plain.rust.graphql.ContentSource,
    sink: suspend (path: String, name: String) -> String,
) {
    var firstSavedPath = ""
    images.forEach { image ->
        val path = if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) image.uri.getFinalPath() else image.uri
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
