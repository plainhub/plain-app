package com.ismartcoding.plain.ui.tools

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.features.media.actions.MediaAction
import com.ismartcoding.plain.features.chat.peer.mediaType
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.*
import com.ismartcoding.plain.models.File as FileEntry
import com.ismartcoding.plain.models.Package as AppPackage
import com.ismartcoding.plain.platform.files.saveFileToDownloads
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.rust.graphql.selectionQuery
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.NavigationCloseIcon
import com.ismartcoding.plain.ui.components.TextFieldDialog
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManagedContentActions(
    item: IData, device: ManagedDeviceViewModel, feature: AppFeatureType, trash: Boolean,
    scope: CoroutineScope, onBusyChange: (Boolean) -> Unit,
    onDismiss: () -> Unit, onOpen: () -> Unit, onChanged: () -> Unit,
) {
    var rename by remember(item.id) { mutableStateOf(false) }
    var renaming by remember(item.id) { mutableStateOf(false) }
    val state by device.state.collectAsState()
    val name = managedTitle(item)
    val deleteText = stringResource(Res.string.delete)
    val trashText = stringResource(Res.string.trash)
    val cancelText = stringResource(Res.string.cancel)
    val capabilities = state.info?.capabilities.orEmpty()
    val canTrash = item is Note && !trash || item is MediaItem && Capability.MEDIA_TRASH in capabilities
    val canDelete = item is FileEntry || item is Feed || item is FeedEntry || item is Note && trash || item is MediaItem && Capability.MEDIA_DELETE in capabilities
    val confirmation = stringResource(if (canTrash) Res.string.managed_trash_confirm else Res.string.managed_delete_confirm, name, state.info?.deviceName.orEmpty())

    if (rename) {
        TextFieldDialog(title = stringResource(Res.string.rename), value = if (item is Feed) name else managedFileName(item), readOnly = renaming, validator = { !renaming },
            onDismissRequest = onDismiss, onConfirm = { value ->
                renaming = true
                scope.launchSafe(onDone = { renaming = false }) {
                    val client = device.client.await()
                    if (item is Feed) client.feeds.update(item.id, name = value)
                    else checkNotNull(client.api.execute(Mutation.renameFile(managedPath(item), value))) { "Rename failed" }
                    onChanged()
                    onDismiss()
                }
            },
        )
        return
    }

    PModalBottomSheet(onDismissRequest = onDismiss) {
        PBottomSheetTopAppBar(title = name, subtitle = state.info?.deviceName.orEmpty(), navigationIcon = { NavigationCloseIcon(onDismiss) })
        PSheetActionCard {
            PSheetActionRow(title = stringResource(Res.string.open), onClick = onOpen)
            if ((item is FileEntry && !item.isDir) || item is MediaItem || item is AppPackage) {
                PSheetActionRow(title = stringResource(Res.string.download), onClick = {
                    onBusyChange(true)
                    onDismiss()
                    scope.launchSafe(onDone = { onBusyChange(false) }) {
                        val fileName = managedFileName(item)
                        val path = device.client.await().cacheFile(managedPath(item), fileName)
                        check(saveFileToDownloads(path, fileName).isNotEmpty()) { "Download failed" }
                        DialogHelper.showSuccess(Res.string.saved)
                    }
                })
            }
            if (item is FileEntry || item is MediaItem || item is Feed) {
                PSheetActionRow(title = stringResource(Res.string.rename), onClick = { rename = true })
            }
            if (item is Feed) PSheetActionRow(title = stringResource(Res.string.feeds_sync_now), onClick = {
                onDismiss()
                scope.launchSafe { device.client.await().feeds.sync(item.id); onChanged() }
            })
            if (item is Note && trash) PSheetActionRow(title = stringResource(Res.string.restore), onClick = {
                onDismiss()
                scope.launchSafe { device.client.await().notes.restoreAsync(setOf(item.id)); onChanged() }
            })
            if (canTrash || canDelete) PSheetActionRow(title = if (canTrash) trashText else deleteText, onClick = {
                onDismiss()
                DialogHelper.showConfirmDialog(
                    title = if (canTrash) trashText else deleteText,
                    message = confirmation,
                    confirmButton = Pair(if (canTrash) trashText else deleteText) {
                        scope.launchSafe {
                            val client = device.client.await()
                            when (item) {
                                is Note -> if (trash) client.notes.deleteAsync(setOf(item.id)) else client.notes.trashAsync(setOf(item.id))
                                is FileEntry -> client.api.execute(Mutation.deleteFiles(listOf(item.path)))
                                is MediaItem -> client.api.execute(Mutation.mediaAction(if (canTrash) MediaAction.TRASH else MediaAction.DELETE, mediaType(feature), selectionQuery(setOf(item.id)), ""))
                                is Feed -> client.feeds.deleteAsync(setOf(item.id))
                                is FeedEntry -> client.api.execute(Mutation.deleteFeedEntries(selectionQuery(setOf(item.id))))
                            }
                            onChanged()
                        }
                    },
                    dismissButton = Pair(cancelText) {}, danger = true,
                )
            })
        }
        VerticalSpace(16.dp)
    }
}
