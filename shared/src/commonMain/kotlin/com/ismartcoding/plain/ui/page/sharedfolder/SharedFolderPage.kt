@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.ChatViewModel
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.db.DSharePeerInfo
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedInfoDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.features.share.SharedLinkClient
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.mdns.MdnsServiceBrowser
import com.ismartcoding.plain.lib.mdns.MdnsServiceSnapshot
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.DownloadTempFileHandle
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.platform.createDownloadTempFile
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.getFileIconPath
import com.ismartcoding.plain.platform.getMimeTypeFromExtension
import com.ismartcoding.plain.platform.importDownloadedFile
import com.ismartcoding.plain.platform.launchUrl
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.BreadcrumbItem
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.page.files.components.BreadcrumbView
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** One visited level of the shared folder tree. Root level has an empty [Crumb.virtualPath]. */
private data class Crumb(val virtualPath: String, val name: String)

/** A successful [SharedLinkClient.fetchSharedInfo] attempt: payload plus the address that worked. */
private class FetchResult(val info: SharedInfoDto, val link: SharedLink)

/** Device address candidates for a share card: message endpoint first, then the paired peer record. */
private fun addressCandidates(msg: DMessageShare): List<SharedLink> {
    val list = mutableListOf(
        SharedLinkClient.linkOf(msg.shareId, msg.urlToken, msg.peerInfo.ip, msg.peerInfo.port),
    )
    PeerCacher.getPeer(msg.peerInfo.id)?.let { peer ->
        if (peer.ip.isNotEmpty() && peer.port > 0) {
            list += SharedLinkClient.linkOf(msg.shareId, msg.urlToken, peer.ip, peer.port)
        }
    }
    return list.distinctBy { "${it.host}:${it.port}" }
}

private fun snapshotDeviceId(snapshot: MdnsServiceSnapshot): String =
    snapshot.txtRecords.firstOrNull { it.startsWith("id=") }?.removePrefix("id=") ?: ""

/**
 * Tries every known address, then falls back to mDNS discovery for the
 * sender's current IP (DHCP changes). Returns null when all attempts fail.
 */
private suspend fun fetchSharedInfoWithFallback(
    msg: DMessageShare,
    virtualPath: String?,
): FetchResult? {
    suspend fun tryAll(links: List<SharedLink>): FetchResult? {
        for (link in links) {
            val fetched = runCatching { withIO { SharedLinkClient.fetchSharedInfo(link, virtualPath) } }.getOrNull()
            if (fetched != null) return FetchResult(fetched, link)
        }
        return null
    }

    tryAll(addressCandidates(msg))?.let { return it }

    // Stale IP: trigger mDNS queries; the resident listener refreshes the
    // snapshot with the sender's current addresses.
    repeat(12) { round ->
        if (round % 4 == 0) MdnsDiscoverManager.browse()
        delay(700)
        val discovered = MdnsServiceBrowser.snapshot()
            .filter { it.complete && snapshotDeviceId(it) == msg.peerInfo.id }
            .flatMap { snapshot -> snapshot.ips.map { SharedLinkClient.linkOf(msg.shareId, msg.urlToken, it, snapshot.port) } }
        if (discovered.isNotEmpty()) {
            tryAll(discovered)?.let { return it }
        }
    }
    return null
}

/**
 * Pulls the share's current name/expiry (the sender may have edited them
 * after the message was sent) plus the working address into the local
 * message row, so the chat card stays accurate. No-op when nothing changed.
 */
private suspend fun syncCardBack(messageId: String, old: DMessageShare, result: FetchResult) {
    val fresh = DMessageShare(
        shareId = old.shareId,
        urlToken = old.urlToken,
        peerInfo = DSharePeerInfo(id = old.peerInfo.id, ip = result.link.host, port = result.link.port),
        name = result.info.name.ifEmpty { old.name },
        itemCount = old.itemCount,
        totalSize = old.totalSize,
        expiresAt = result.info.expiresAtInstant ?: old.expiresAt,
    )
    if (fresh.name == old.name && fresh.expiresAt == old.expiresAt &&
        fresh.peerInfo.ip == old.peerInfo.ip && fresh.peerInfo.port == old.peerInfo.port
    ) {
        return
    }
    withIO {
        val chat = ChatManager.getChatItem(messageId) ?: return@withIO
        chat.content.value = fresh
        AppDatabase.instance.chatDao().updateData(ChatItemDataUpdate(messageId, chat.content))
    }
    ChatViewModel.onMessageUpdated(messageId)
}

/**
 * Native browser for a shared folder card message, styled after FilesPage.
 * Browses via the guest `sharedInfo` query, saves selected entries through
 * `/fs` (files) and `/zip/dir` (folders) into the app file store. Images are
 * previewed with the shared MediaPreviewer after an on-tap download (the
 * guest `/fs` endpoint is self-signed HTTPS, so Coil cannot load it directly).
 * Falls back to mDNS when the sender's IP changed, and syncs fresh share data
 * back into the local chat card on success.
 */
@Composable
fun SharedFolderPage(
    navController: NavHostController,
    messageId: String,
) {
    val scope = rememberCoroutineScope()

    var shareMsg by remember { mutableStateOf<DMessageShare?>(null) }
    var crumbs by remember { mutableStateOf(listOf<Crumb>()) }
    var info by remember { mutableStateOf<SharedInfoDto?>(null) }
    var activeLink by remember { mutableStateOf<SharedLink?>(null) }
    var error by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<SharedFileDto>()) }
    var progress by remember { mutableStateOf(mapOf<String, Float>()) }
    var saving by remember { mutableStateOf(false) }
    var previewLoading by remember { mutableStateOf(setOf<String>()) }

    val previewerState = rememberPreviewerState(scope = scope, pageCount = { MediaPreviewData.items.size })
    val previewHandles = remember { mutableListOf<DownloadTempFileHandle>() }
    DisposableEffect(Unit) {
        onDispose {
            previewHandles.forEach { it.delete() }
            previewHandles.clear()
        }
    }

    LaunchedEffect(messageId) {
        val chat = withIO { ChatManager.getChatItem(messageId) }
        shareMsg = chat?.content?.value as? DMessageShare
    }

    val currentPath = crumbs.lastOrNull()?.virtualPath ?: ""

    fun load(path: String) {
        val msg = shareMsg ?: return
        scope.launch {
            loading = true
            error = false
            val result = fetchSharedInfoWithFallback(msg, path.takeIf { it.isNotEmpty() })
            if (result == null) {
                error = true
                loading = false
                return@launch
            }
            info = result.info
            activeLink = result.link
            syncCardBack(messageId, msg, result)
            loading = false
        }
    }

    LaunchedEffect(shareMsg, currentPath) {
        if (shareMsg != null) load(currentPath)
    }

    /**
     * Saves every entry in [entries] sequentially — files via `/fs`, folders
     * via a per-folder `/zip/dir` download — reporting per-entry progress.
     * [finish] runs after the last entry (or failure) with the success count.
     */
    fun saveEntries(entries: List<SharedFileDto>, finish: (Int) -> Unit) {
        val urlToken = info?.urlToken ?: return
        val link = activeLink ?: return
        scope.launch {
            var ok = 0
            entries.forEach { entry ->
                progress = progress + (entry.virtualPath to 0f)
                val result = runCatching {
                    withIO {
                        val isZip = entry.isDir
                        val handle = createDownloadTempFile(entry.virtualPath.hashCode().toString())
                        try {
                            val downloadUrl = if (isZip) {
                                SharedLinkClient.zipDirUrl(link, urlToken, entry.virtualPath)
                            } else {
                                SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath)
                            }
                            SharedLinkClient.downloadTo(
                                downloadUrl,
                                { buffer, length -> handle.write(buffer, 0, length) },
                            ) { done, total ->
                                if (total > 0) progress = progress + (entry.virtualPath to (done.toFloat() / total))
                            }
                            handle.close()
                            val fileName = if (isZip) "${entry.name}.zip" else entry.name
                            val mime = if (isZip) {
                                "application/zip"
                            } else {
                                entry.mimeType.ifEmpty { getMimeTypeFromExtension(fileName.getFilenameExtension()) }
                            }
                            importDownloadedFile(handle, fileName, mime)
                        } catch (e: Exception) {
                            handle.delete()
                            throw e
                        }
                    }
                }
                if (result.isSuccess) ok++
                progress = progress - entry.virtualPath
            }
            finish(ok)
        }
    }

    /**
     * Downloads an image entry into a preview temp file (named with the
     * original extension so the previewer detects the format), then opens it
     * in the shared MediaPreviewer. Temp files are cleaned up on page exit.
     */
    fun openImagePreview(entry: SharedFileDto) {
        val urlToken = info?.urlToken ?: return
        val link = activeLink ?: return
        if (previewLoading.contains(entry.virtualPath)) return
        previewLoading = previewLoading + entry.virtualPath
        scope.launch {
            val handle = createDownloadTempFile("preview_${entry.name}")
            val ok = runCatching {
                withIO {
                    SharedLinkClient.downloadTo(
                        SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath),
                        { buffer, length -> handle.write(buffer, 0, length) },
                    )
                    handle.close()
                }
            }.isSuccess
            previewLoading = previewLoading - entry.virtualPath
            if (!ok) {
                handle.delete()
                DialogHelper.showErrorDialog(com.ismartcoding.plain.platform.LocaleHelper.getString(Res.string.cannot_load_share))
                return@launch
            }
            previewHandles.add(handle)
            MediaPreviewData.items = listOf(PreviewItem(id = entry.virtualPath, path = handle.filePath))
            previewerState.open(0)
        }
    }

    fun browserUrl(): String? {
        val msg = shareMsg ?: return null
        val link = activeLink ?: SharedLinkClient.linkOf(msg.shareId, msg.urlToken, msg.peerInfo.ip, msg.peerInfo.port)
        return SharedLinkClient.pageUrl(link)
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = info?.name ?: shareMsg?.name ?: "",
                actions = {
                    PCapsuleMoreClose(
                        onClose = { navController.popBackStack() },
                    ) { dismiss ->
                        PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.open_in_browser)) },
                            onClick = {
                                dismiss()
                                browserUrl()?.let { launchUrl(it) }
                            },
                        )
                        PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.copy)) },
                            onClick = {
                                dismiss()
                                browserUrl()?.let {
                                    setClipboardText("", it)
                                    DialogHelper.showSuccess(Res.string.copied)
                                }
                            },
                        )
                    }
                },
            )
        },
    ) { _ ->
        val entries = info?.entries
            ?.sortedWith(compareBy<SharedFileDto> { !it.isDir }.thenBy { it.name.lowercase() })
            ?: emptyList()
        val breadcrumbs = buildList {
            add(BreadcrumbItem(info?.name ?: "/", ""))
            crumbs.forEach { add(BreadcrumbItem(it.name, it.virtualPath)) }
        }
        Column(modifier = Modifier.fillMaxSize()) {
            MetaBanner(info)
            if (error) {
                ErrorBanner(onRetry = { load(currentPath) })
            }
            BreadcrumbView(
                breadcrumbs = breadcrumbs,
                selectedIndex = breadcrumbs.lastIndex,
                onItemClick = { item ->
                    if (selectMode) return@BreadcrumbView
                    val index = breadcrumbs.indexOfFirst { it.path == item.path }
                    if (index in 0 until breadcrumbs.lastIndex) crumbs = crumbs.take(index)
                },
            )
            when {
                shareMsg == null && !loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.cannot_load_share),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                loading && info == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                entries.isEmpty() && !error && info != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.shared_folder_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                entries.isNotEmpty() -> {
                    if (!selectMode) {
                        Text(
                            text = stringResource(Res.string.select_items_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(entries, key = { it.virtualPath }) { entry ->
                            EntryRow(
                                entry = entry,
                                selectMode = selectMode,
                                selected = selected.contains(entry),
                                progress = progress[entry.virtualPath],
                                previewLoading = previewLoading.contains(entry.virtualPath),
                                onClick = {
                                    when {
                                        selectMode -> {
                                            selected = if (selected.contains(entry)) selected - entry else selected + entry
                                        }
                                        entry.isDir -> crumbs = crumbs + Crumb(entry.virtualPath, entry.name)
                                        entry.mimeType.startsWith("image/") || entry.name.isImageFast() -> openImagePreview(entry)
                                    }
                                },
                                onLongClick = {
                                    if (!selectMode) {
                                        selectMode = true
                                        selected = setOf(entry)
                                    }
                                },
                            )
                        }
                    }
                    if (selectMode) {
                        BottomActionButtons {
                            PFilledButton(
                                text = stringResource(Res.string.save_selected, selected.size),
                                onClick = {
                                    saving = true
                                    val targets = selected.sortedWith(compareBy<SharedFileDto> { !it.isDir }.thenBy { it.name.lowercase() })
                                    saveEntries(targets) { okCount ->
                                        saving = false
                                        if (okCount > 0) DialogHelper.showSuccess(Res.string.saved_to_files)
                                        selectMode = false
                                        selected = emptySet()
                                        progress = emptyMap()
                                    }
                                },
                                enabled = !saving && selected.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                            )
                            POutlinedButton(
                                text = stringResource(Res.string.download_all_zip),
                                onClick = {
                                    saving = true
                                    val rootName = crumbs.lastOrNull()?.name ?: info?.name ?: "shared"
                                    saveEntries(listOf(SharedFileDto(name = rootName, virtualPath = currentPath, isDir = true))) { okCount ->
                                        saving = false
                                        if (okCount > 0) DialogHelper.showSuccess(Res.string.saved_to_files)
                                        selectMode = false
                                        selected = emptySet()
                                        progress = emptyMap()
                                    }
                                },
                                enabled = !saving,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    MediaPreviewer(state = previewerState)
}

@Composable
private fun MetaBanner(info: SharedInfoDto?) {
    if (info == null) return
    val parts = mutableListOf(stringResource(Res.string.folder_card_items, info.entries.size))
    val expiresAt = info.expiresAtInstant
    if (expiresAt != null) {
        val expired = expiresAt < TimeHelper.now()
        val dateText = expiresAt.formatDateTime()
        parts.add(
            if (expired) dateText
            else stringResource(Res.string.share_expires_on, dateText),
        )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.cardBackgroundNormal,
    ) {
        Text(
            text = parts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ErrorBanner(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.cannot_load_share),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            POutlinedButton(
                text = stringResource(Res.string.retry),
                onClick = onRetry,
                type = ButtonType.DANGER,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** FilesPage-style file row: rounded card, 48dp type icon, name + size, select checkbox. */
@Composable
private fun EntryRow(
    entry: SharedFileDto,
    selectMode: Boolean,
    selected: Boolean,
    progress: Float?,
    previewLoading: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(
                if (selectMode && selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.cardBackgroundNormal
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectMode) {
                Checkbox(checked = selected, onCheckedChange = null)
                Box(modifier = Modifier.size(8.dp))
            }
            AsyncImage(
                model = if (entry.isDir) {
                    getFileIconPath("folder")
                } else {
                    getFileIconPath(entry.name.getFilenameExtension())
                },
                modifier = Modifier.size(48.dp),
                alignment = Alignment.Center,
                contentDescription = entry.name,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!entry.isDir && entry.size > 0) {
                    Text(
                        text = entry.size.formatBytes(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        when {
            progress != null -> LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            previewLoading -> LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}
