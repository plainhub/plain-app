@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedInfoDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.features.share.SharedLinkClient
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.DownloadTempFileHandle
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.platform.getDownloadsDirPath
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.launchUrl
import com.ismartcoding.plain.platform.streamZipToSink
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.SaveToSheet
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.BreadcrumbItem
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.page.files.components.BreadcrumbView
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** One visited level of the shared folder tree. Root level has an empty [Crumb.virtualPath]. */
private data class Crumb(val virtualPath: String, val name: String)

/**
 * Native browser for a shared folder card message, styled after FilesPage.
 * Data resolution lives in SharedFolderLoader.kt, the download/sync engine in
 * SharedFolderTransfer.kt, and the private row/banner pieces in
 * SharedFolderComponents.kt — this file is page orchestration only.
 */
@Composable
fun SharedFolderPage(
    navController: NavHostController,
    messageId: String,
) {
    val scope = rememberCoroutineScope()

    var shareMsg by remember { mutableStateOf<DMessageShare?>(null) }
    var crumbs by remember { mutableStateOf(listOf<Crumb>()) }
    var rootInfo by remember { mutableStateOf<SharedInfoDto?>(null) }

    // Per-directory content cache: entering a visited directory renders
    // instantly from here while a background refresh runs.
    val dirCache = remember { mutableStateMapOf<String, SharedInfoDto>() }
    var loadingPath by remember { mutableStateOf<String?>(null) }
    var errorPath by remember { mutableStateOf<String?>(null) }
    var activeLink by remember { mutableStateOf<SharedLink?>(null) }
    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<SharedFileDto>()) }
    var progress by remember { mutableStateOf(mapOf<String, Float>()) }
    var showSaveSelectedSheet by remember { mutableStateOf(false) }
    var previewLoading by remember { mutableStateOf(setOf<String>()) }
    var downloadTarget by remember { mutableStateOf<SharedFileDto?>(null) }
    var syncStatus by remember { mutableStateOf<String?>(null) }

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
        if (loadingPath == path) return
        scope.launch {
            loadingPath = path
            errorPath = null
            val result = fetchSharedInfoWithFallback(msg, path.takeIf { it.isNotEmpty() })
            // User navigated elsewhere meanwhile: drop the stale result.
            if (loadingPath != path) return@launch
            loadingPath = null
            if (result == null) {
                errorPath = path
                return@launch
            }
            dirCache[path] = result.info
            activeLink = result.link
            if (path.isEmpty()) rootInfo = result.info
            syncCardBack(messageId, msg, result)
        }
    }

    LaunchedEffect(shareMsg, currentPath) {
        if (shareMsg != null && dirCache[currentPath] == null && loadingPath != currentPath) load(currentPath)
    }

    /** Report download success/failure once an operation finishes. */
    fun reportResult(ok: Boolean) {
        if (ok) {
            DialogHelper.showSuccess(Res.string.saved)
        } else {
            DialogHelper.showErrorDialog(LocaleHelper.getString(Res.string.download_failed))
        }
    }

    /**
     * Downloads an image/video entry into a preview temp file, then opens it
     * in the shared MediaPreviewer (videos autoplay). Temp files are cleaned
     * up on page exit.
     */
    fun openMediaPreview(entry: SharedFileDto) {
        val urlToken = rootInfo?.urlToken ?: return
        val link = activeLink ?: return
        if (previewLoading.contains(entry.virtualPath)) return
        previewLoading = previewLoading + entry.virtualPath
        scope.launch {
            val handle = SharedFolderTransfer.downloadPreviewFile(link, urlToken, entry)
            previewLoading = previewLoading - entry.virtualPath
            if (handle == null) {
                DialogHelper.showErrorDialog(LocaleHelper.getString(Res.string.cannot_load_share))
                return@launch
            }
            previewHandles.add(handle)
            MediaPreviewData.items = listOf(PreviewItem(id = entry.virtualPath, path = handle.filePath))
            previewerState.open(0)
        }
    }

    fun downloadFileToDownloads(entry: SharedFileDto) {
        val urlToken = rootInfo?.urlToken ?: return
        val link = activeLink ?: return
        progress = progress + (entry.virtualPath to 0f)
        scope.launch {
            val saved = SharedFolderTransfer.downloadFileToDownloads(link, urlToken, entry)
            progress = progress - entry.virtualPath
            reportResult(saved.isNotEmpty())
        }
    }

    fun downloadFileToDir(entry: SharedFileDto, dir: String) {
        val urlToken = rootInfo?.urlToken ?: return
        val link = activeLink ?: return
        progress = progress + (entry.virtualPath to 0f)
        scope.launch {
            val ok = SharedFolderTransfer.downloadFileToDir(link, urlToken, entry, dir)
            progress = progress - entry.virtualPath
            reportResult(ok)
        }
    }

    fun downloadZipToDownloads(entry: SharedFileDto) {
        val urlToken = rootInfo?.urlToken ?: return
        val link = activeLink ?: return
        progress = progress + (entry.virtualPath to 0f)
        scope.launch {
            val saved = SharedFolderTransfer.downloadZipToDownloads(link, urlToken, entry)
            progress = progress - entry.virtualPath
            reportResult(saved.isNotEmpty())
        }
    }

    /** Mirror a shared directory into a local [baseDir] subfolder, no zip. */
    fun syncDirTo(entry: SharedFileDto, baseDir: String) {
        val urlToken = rootInfo?.urlToken ?: return
        val link = activeLink ?: return
        val targetDir = baseDir.trimEnd('/') + "/" + entry.name
        syncStatus = LocaleHelper.getString(Res.string.syncing_files)
        scope.launch {
            val ok = runCatching {
                withIO { SharedFolderTransfer.syncDirectory(link, urlToken, entry.virtualPath, "", targetDir) }
            }.isSuccess
            syncStatus = null
            reportResult(ok)
        }
    }

    fun sortedSelection(): List<SharedFileDto> =
        selected.sortedWith(compareBy<SharedFileDto> { !it.isDir }.thenBy { it.name.lowercase() })

    /** Saves the current selection into the public Downloads dir. */
    fun saveSelectionToDownloads() {
        val urlToken = rootInfo?.urlToken ?: return
        val link = activeLink ?: return
        if (selected.isEmpty()) return
        syncStatus = LocaleHelper.getString(Res.string.syncing_files)
        scope.launch {
            val okCount = SharedFolderTransfer.saveEntriesToDownloads(link, urlToken, sortedSelection(), getDownloadsDirPath())
            syncStatus = null
            reportResult(okCount == selected.size && selected.isNotEmpty())
            selectMode = false
            selected = emptySet()
        }
    }

    /** Saves the current selection into a picked directory. */
    fun saveSelectionToDir(dir: String) {
        val urlToken = rootInfo?.urlToken ?: return
        val link = activeLink ?: return
        if (selected.isEmpty()) return
        syncStatus = LocaleHelper.getString(Res.string.syncing_files)
        scope.launch {
            val okCount = SharedFolderTransfer.saveEntriesToDir(link, urlToken, sortedSelection(), dir)
            syncStatus = null
            reportResult(okCount == selected.size && selected.isNotEmpty())
            selectMode = false
            selected = emptySet()
        }
    }

    /** Zips the selected file entries into one archive in the Downloads dir. */
    fun zipSelection() {
        val urlToken = rootInfo?.urlToken ?: return
        val link = activeLink ?: return
        val zipName = (rootInfo?.name ?: "shared") + "_selected.zip"
        val dest = "${getDownloadsDirPath().trimEnd('/')}/PlainApp/$zipName"
        syncStatus = LocaleHelper.getString(Res.string.syncing_files)
        scope.launch {
            val ok = runCatching {
                withIO { SharedFolderTransfer.zipEntriesTo(link, urlToken, sortedSelection(), dest) }
            }.isSuccess
            syncStatus = null
            reportResult(ok)
        }
    }

    fun browserUrl(): String? {
        val msg = shareMsg ?: return null
        val link = activeLink ?: SharedLinkClient.linkOf(msg.shareId, msg.urlToken, msg.peerInfo.ip, msg.peerInfo.port)
        return SharedLinkClient.pageUrl(link)
    }

    val currentInfo = dirCache[currentPath]
    val entries = currentInfo?.entries
        ?.sortedWith(compareBy<SharedFileDto> { !it.isDir }.thenBy { it.name.lowercase() })
        ?: emptyList()
    val active = activeLink
    val pathLoading = loadingPath == currentPath
    val pathError = errorPath == currentPath

    PScaffold(
        topBar = {
            PTopAppBar(
                title = if (selectMode) {
                    stringResource(Res.string.x_selected, selected.size)
                } else {
                    rootInfo?.name ?: shareMsg?.name ?: ""
                },
                navigationIcon = if (selectMode) {
                    { NavigationCloseIcon { selectMode = false; selected = emptySet() } }
                } else {
                    null
                },
                actions = {
                    if (selectMode) {
                        PTextButton(
                            text = stringResource(if (selected.containsAll(entries)) Res.string.unselect_all else Res.string.select_all),
                            onClick = {
                                if (selected.containsAll(entries)) selected = emptySet() else selected = entries.toSet()
                            },
                        )
                    } else {
                        PCapsuleMoreClose(
                            onClose = { navController.popBackStack() },
                        ) { dismiss ->
                            PSheetActionRow(Res.drawable.chrome, stringResource(Res.string.open_in_browser)) {
                                dismiss()
                                browserUrl()?.let { launchUrl(it) }
                            }
                            PSheetActionRow(Res.drawable.copy, stringResource(Res.string.copy)) {
                                dismiss()
                                browserUrl()?.let {
                                    setClipboardText("", it)
                                    DialogHelper.showSuccess(Res.string.copied)
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        val breadcrumbs = buildList {
            add(BreadcrumbItem(rootInfo?.name ?: "/", ""))
            crumbs.forEach { add(BreadcrumbItem(it.name, it.virtualPath)) }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            MetaBanner(rootInfo)
            if (pathError) {
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
            if (syncStatus != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = syncStatus!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            when {
                shareMsg == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.cannot_load_share),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                currentInfo == null && pathLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                currentInfo == null && pathError -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.cannot_load_share),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                entries.isEmpty() && currentInfo != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.shared_folder_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                entries.isNotEmpty() && active != null -> {
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
                                link = active,
                                urlToken = rootInfo?.urlToken ?: "",
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
                                        entry.mimeType.startsWith("image/") || entry.mimeType.startsWith("video/") ||
                                            entry.name.isImageFast() || entry.name.isVideoFast() -> openMediaPreview(entry)
                                    }
                                },
                                onLongClick = {
                                    if (!selectMode) {
                                        selectMode = true
                                        selected = setOf(entry)
                                    }
                                },
                                onDownload = { downloadTarget = entry },
                            )
                        }
                    }
                    if (selectMode) {
                        BottomActionButtons {
                            PFilledButton(
                                text = stringResource(Res.string.save_selected, selected.size),
                                onClick = { showSaveSelectedSheet = true },
                                enabled = selected.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    downloadTarget?.let { target ->
        SaveToSheet(
            title = target.name,
            onDismiss = { downloadTarget = null },
            onDownloads = {
                downloadTarget = null
                if (target.isDir) syncDirTo(target, "${getDownloadsDirPath().trimEnd('/')}/PlainApp")
                else downloadFileToDownloads(target)
            },
            onDirectory = { dir ->
                downloadTarget = null
                if (target.isDir) syncDirTo(target, dir) else downloadFileToDir(target, dir)
            },
            onZip = target.takeIf { it.isDir }?.let { entry -> ({ downloadTarget = null; downloadZipToDownloads(entry) }) },
        )
    }

    if (showSaveSelectedSheet) {
        val fileCount = selected.count { !it.isDir }
        SaveToSheet(
            title = stringResource(Res.string.save_selected, selected.size),
            onDismiss = { showSaveSelectedSheet = false },
            onDownloads = {
                showSaveSelectedSheet = false
                saveSelectionToDownloads()
            },
            onDirectory = { dir ->
                showSaveSelectedSheet = false
                saveSelectionToDir(dir)
            },
            onZip = {
                showSaveSelectedSheet = false
                zipSelection()
            },
        )
    }

    MediaPreviewer(state = previewerState)
}
