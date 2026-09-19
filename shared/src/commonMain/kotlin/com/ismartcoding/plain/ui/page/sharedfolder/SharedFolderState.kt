package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.features.download.isTerminalDownloadStatus
import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedFolderBatchTask
import com.ismartcoding.plain.features.share.SharedFolderDownloadEngine
import com.ismartcoding.plain.features.share.SharedInfoDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.features.share.SharedLinkClient
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.DownloadTempFileHandle
import com.ismartcoding.plain.platform.LocaleHelper
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
 * the in-page media preview. Download/sync transfers are enqueued into the
 * process-level [SharedFolderDownloadEngine] and survive page exit; progress
 * is read back from [DownloadCenter].
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

    fun isPreviewLoading(entry: SharedFileDto): Boolean = previewLoading.contains(entry.virtualPath)

    /** Batch tasks belonging to this share, in insertion order. */
    fun shareTasks(): List<SharedFolderBatchTask> = DownloadCenter.progress.value.values
        .filterIsInstance<SharedFolderBatchTask>()
        .filter { it.messageId == messageId }

    /** True while a batch covering [entry] is queued or downloading. */
    fun isEntryBusy(entry: SharedFileDto): Boolean = DownloadCenter.progress.value.values.any {
        it is SharedFolderBatchTask && it.messageId == messageId &&
            !it.status.isTerminalDownloadStatus() &&
            it.entries.any { e -> e.virtualPath == entry.virtualPath }
    }

    fun browserUrl(): String? {
        val msg = shareMsg ?: return null
        val link = activeLink ?: addressCandidates(msg).firstOrNull() ?: return null
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

    /** Enqueue one entry save (file streams, dir mirrors) into [targetDir]; "" = public Downloads. */
    fun enqueueEntry(target: SharedFileDto, targetDir: String) {
        val (link, urlToken) = transferContext() ?: return
        if (target.isDir) {
            SharedFolderDownloadEngine.enqueueDirSync(messageId, link, urlToken, target, targetDir)
        } else {
            SharedFolderDownloadEngine.enqueueFile(messageId, link, urlToken, target, targetDir)
        }
    }

    /** Enqueue one directory entry as a single zip archive. */
    fun enqueueEntryZip(target: SharedFileDto) {
        val (link, urlToken) = transferContext() ?: return
        SharedFolderDownloadEngine.enqueueZip(messageId, link, urlToken, listOf(target), "${target.name}.zip")
    }

    /** Enqueue the current multi-selection into [targetDir]; "" = public Downloads. */
    fun enqueueSelection(targetDir: String) {
        val (link, urlToken) = transferContext() ?: return
        if (selected.isEmpty()) return
        SharedFolderDownloadEngine.enqueueMulti(messageId, link, urlToken, sortedSelection(), targetDir)
        clearSelection()
    }

    /** Enqueue the current selection as one zip archive into Downloads/PlainApp. */
    fun enqueueSelectionZip() {
        val (link, urlToken) = transferContext() ?: return
        if (selected.isEmpty()) return
        val zipName = (rootInfo?.name ?: "shared") + "_selected.zip"
        SharedFolderDownloadEngine.enqueueZip(messageId, link, urlToken, sortedSelection(), zipName)
        clearSelection()
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

    private fun sortedSelection(): List<SharedFileDto> =
        selected.sortedWith(compareBy<SharedFileDto> { !it.isDir }.thenBy { it.name.lowercase() })
}
