package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.getDownloadsDirPath
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.MediaPreviewData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** One visited level of the shared folder tree. Root level has an empty [Crumb.virtualPath]. */
internal data class Crumb(val virtualPath: String, val name: String)

/**
 * State holder for [SharedFolderPage]: owns the share/navigation state and
 * wraps the [SharedFolderTransfer] engines with progress, status and result
 * reporting. The page composable is layout wiring only.
 */
internal class SharedFolderState(
    private val messageId: String,
    private val scope: CoroutineScope,
    private val previewerState: MediaPreviewerState,
) {
    var shareMsg by mutableStateOf<DMessageShare?>(null)
        private set
    var rootInfo by mutableStateOf<SharedInfoDto?>(null)
        private set
    var activeLink by mutableStateOf<SharedLink?>(null)
        private set
    private var loadingPath by mutableStateOf<String?>(null)
    private var errorPath by mutableStateOf<String?>(null)

    var crumbs by mutableStateOf(listOf<Crumb>())
        private set
    var selectMode by mutableStateOf(false)
    var selected by mutableStateOf(setOf<SharedFileDto>())
    var downloadTarget by mutableStateOf<SharedFileDto?>(null)
    var showSaveSelectedSheet by mutableStateOf(false)
    var syncStatus by mutableStateOf<String?>(null)
        private set
    private var progress by mutableStateOf(mapOf<String, Float>())
    private var previewLoading by mutableStateOf(setOf<String>())

    /** Per-directory content cache: entering a visited directory renders instantly from here. */
    private val dirCache = mutableStateMapOf<String, SharedInfoDto>()

    private val previewHandles = mutableListOf<DownloadTempFileHandle>()

    val currentPath: String get() = crumbs.lastOrNull()?.virtualPath ?: ""
    val currentInfo: SharedInfoDto? get() = dirCache[currentPath]
    val pathLoading: Boolean get() = loadingPath == currentPath
    val pathError: Boolean get() = errorPath == currentPath
    val entries: List<SharedFileDto>
        get() = currentInfo?.entries?.sortedWith(
            compareBy<SharedFileDto> { !it.isDir }.thenBy { it.name.lowercase() },
        ) ?: emptyList()

    fun loadMessage() {
        scope.launch {
            val chat = withIO { ChatManager.getChatItem(messageId) }
            shareMsg = chat?.content?.value as? DMessageShare
        }
    }

    fun ensureLoaded() {
        if (shareMsg != null && dirCache[currentPath] == null && loadingPath != currentPath) load(currentPath)
    }

    fun retry() = load(currentPath)

    private fun load(path: String) {
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

    fun onCrumbClick(path: String) {
        if (selectMode) return
        crumbs = if (path.isEmpty()) {
            emptyList()
        } else {
            val index = crumbs.indexOfFirst { it.virtualPath == path }
            if (index >= 0) crumbs.take(index + 1) else crumbs
        }
    }

    fun onEntryClick(entry: SharedFileDto) {
        when {
            selectMode -> selected = if (selected.contains(entry)) selected - entry else selected + entry
            entry.isDir -> crumbs = crumbs + Crumb(entry.virtualPath, entry.name)
            entry.mimeType.startsWith("image/") || entry.mimeType.startsWith("video/") ||
                entry.name.isImageFast() || entry.name.isVideoFast() -> openMediaPreview(entry)
        }
    }

    fun onEntryLongClick(entry: SharedFileDto) {
        if (!selectMode) {
            selectMode = true
            selected = setOf(entry)
        }
    }

    fun clearSelection() {
        selectMode = false
        selected = emptySet()
    }

    fun toggleSelectAll() {
        selected = if (selected.containsAll(entries)) emptySet() else entries.toSet()
    }

    fun progressOf(entry: SharedFileDto): Float? = progress[entry.virtualPath]

    fun isPreviewLoading(entry: SharedFileDto): Boolean = previewLoading.contains(entry.virtualPath)

    fun browserUrl(): String? {
        val msg = shareMsg ?: return null
        val link = activeLink ?: SharedLinkClient.linkOf(msg.shareId, msg.urlToken, msg.peerInfo.ip, msg.peerInfo.port)
        return SharedLinkClient.pageUrl(link)
    }

    /**
     * Downloads an image/video entry into a preview temp file, then opens it
     * in the shared MediaPreviewer (videos autoplay). Temp files are cleaned
     * up on page exit.
     */
    fun openMediaPreview(entry: SharedFileDto) {
        val (link, urlToken) = transferContext() ?: return
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
        val (link, urlToken) = transferContext() ?: return
        progress = progress + (entry.virtualPath to 0f)
        scope.launch {
            val saved = SharedFolderTransfer.downloadFileToDownloads(link, urlToken, entry)
            progress = progress - entry.virtualPath
            reportResult(saved.isNotEmpty())
        }
    }

    fun downloadFileToDir(entry: SharedFileDto, dir: String) {
        val (link, urlToken) = transferContext() ?: return
        progress = progress + (entry.virtualPath to 0f)
        scope.launch {
            val ok = SharedFolderTransfer.downloadFileToDir(link, urlToken, entry, dir)
            progress = progress - entry.virtualPath
            reportResult(ok)
        }
    }

    fun downloadZipToDownloads(entry: SharedFileDto) {
        val (link, urlToken) = transferContext() ?: return
        progress = progress + (entry.virtualPath to 0f)
        scope.launch {
            val saved = SharedFolderTransfer.downloadZipToDownloads(link, urlToken, entry)
            progress = progress - entry.virtualPath
            reportResult(saved.isNotEmpty())
        }
    }

    /** Mirror a shared directory into a local [baseDir] subfolder, no zip. */
    fun syncDirTo(entry: SharedFileDto, baseDir: String) {
        val (link, urlToken) = transferContext() ?: return
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

    /** Saves the current selection into the public Downloads dir. */
    fun saveSelectionToDownloads() {
        val (link, urlToken) = transferContext() ?: return
        if (selected.isEmpty()) return
        syncStatus = LocaleHelper.getString(Res.string.syncing_files)
        scope.launch {
            val okCount = SharedFolderTransfer.saveEntriesToDownloads(link, urlToken, sortedSelection(), getDownloadsDirPath())
            syncStatus = null
            reportResult(okCount == selected.size && selected.isNotEmpty())
            clearSelection()
        }
    }

    /** Saves the current selection into a picked directory. */
    fun saveSelectionToDir(dir: String) {
        val (link, urlToken) = transferContext() ?: return
        if (selected.isEmpty()) return
        syncStatus = LocaleHelper.getString(Res.string.syncing_files)
        scope.launch {
            val okCount = SharedFolderTransfer.saveEntriesToDir(link, urlToken, sortedSelection(), dir)
            syncStatus = null
            reportResult(okCount == selected.size && selected.isNotEmpty())
            clearSelection()
        }
    }

    /** Zips the selected file entries into one archive in the Downloads dir. */
    fun zipSelection() {
        val (link, urlToken) = transferContext() ?: return
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

    fun clearPreviewHandles() {
        previewHandles.forEach { it.delete() }
        previewHandles.clear()
    }

    /** Working link + url token, or null when the share hasn't resolved yet. */
    private fun transferContext(): Pair<SharedLink, String>? {
        val urlToken = rootInfo?.urlToken ?: return null
        val link = activeLink ?: return null
        return link to urlToken
    }

    /** Report download success/failure once an operation finishes. */
    private fun reportResult(ok: Boolean) {
        if (ok) {
            DialogHelper.showSuccess(Res.string.saved)
        } else {
            DialogHelper.showErrorDialog(LocaleHelper.getString(Res.string.download_failed))
        }
    }

    private fun sortedSelection(): List<SharedFileDto> =
        selected.sortedWith(compareBy<SharedFileDto> { !it.isDir }.thenBy { it.name.lowercase() })
}
