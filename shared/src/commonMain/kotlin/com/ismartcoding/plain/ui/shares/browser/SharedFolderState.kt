package com.ismartcoding.plain.ui.shares.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.models.MessageShare
import com.ismartcoding.plain.features.downloads.isTerminalDownloadStatus
import com.ismartcoding.plain.features.shares.SharedFileDto
import com.ismartcoding.plain.features.shares.SharedFolderDownloadEngine
import com.ismartcoding.plain.features.shares.SharedInfoDto
import com.ismartcoding.plain.features.shares.SharedLink
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isPdfFile
import com.ismartcoding.plain.lib.extensions.isTextFile
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.files.deleteFileAt
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.files.components.openFileByType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.features.shares.SharedFolderDownloadClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** One visited level of the shared folder tree. Root level has an empty [Crumb.virtualPath]. */
internal data class Crumb(val virtualPath: String, val name: String)

internal class SharedFolderState(
    private val messageId: String,
    private val navController: NavHostController,
    private val scope: CoroutineScope,
    private val previewerState: MediaPreviewerState,
    val source: ContentSource,
) {
    val downloads = if (source is ContentSource.Local) SharedFolderDownloadEngine else SharedFolderDownloadClient(source.api, scope)
    var shareMsg by mutableStateOf<MessageShare?>(null)
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

    private val previewHandles = mutableListOf<String>()

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
            try {
                val chat = withIO { source.chats.getChatItem(messageId) }
                shareMsg = chat?.messageContent?.value as? MessageShare
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { DialogHelper.showMessage(error) }
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
            val result = try {
                source.sharedLinks.browse(messageId, msg, path.takeIf { it.isNotEmpty() })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                DialogHelper.showMessage(error)
                null
            }
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
            shareMsg = result.card
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
            entry.opensLikeLocal() -> openEntryPreview(entry)
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

    /** True while a batch covering [entry] is queued or downloading. */
    fun isEntryBusy(entry: SharedFileDto): Boolean = downloads.progress.value.values.any {
        it.messageId == messageId &&
            !it.status.isTerminalDownloadStatus() &&
            it.entries.any { e -> e.virtualPath == entry.virtualPath }
    }

    fun withBrowserUrl(action: (String) -> Unit) {
        val msg = shareMsg ?: return
        scope.launch {
            try {
                val link = activeLink ?: source.sharedLinks.initialLink(messageId, msg)
                action(link.pageUrl)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { DialogHelper.showErrorDialog(LocaleHelper.getString(Res.string.cannot_load_share)) }
        }
    }

    fun openEntryPreview(entry: SharedFileDto) {
        val (link, urlToken) = transferContext() ?: return
        if (previewLoading.contains(entry.virtualPath)) return
        previewLoading = previewLoading + entry.virtualPath
        scope.launch {
            val path = try {
                source.sharedLinks.cacheFile(link, urlToken, entry)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                DialogHelper.showMessage(error)
                return@launch
            } finally {
                previewLoading = previewLoading - entry.virtualPath
            }
            val inPagePreview = !(path.isAudioFast() || path.isTextFile() || path.isPdfFile())
            if (inPagePreview && source is ContentSource.Local) previewHandles.add(path)
            openFileByType(
                path = path,
                navController = navController,
                source = source,
                name = entry.name,
                mediaHint = entry.mimeType.startsWith("image/") || entry.mimeType.startsWith("video/"),
                onPreviewMedia = {
                    MediaPreviewData.items = listOf(PreviewItem(id = entry.virtualPath, path = path, source = source, size = entry.size))
                    scope.launch { previewerState.open(0) }
                },
            )
        }
    }

    /** Enqueue one entry save (file streams, dir mirrors) into [targetDir]; "" = public Downloads. */
    fun enqueueEntry(target: SharedFileDto, targetDir: String) {
        val (link, urlToken) = transferContext() ?: return
        if (target.isDir) {
            downloads.enqueueDirSync(messageId, link, urlToken, target, targetDir)
        } else {
            downloads.enqueueFile(messageId, link, urlToken, target, targetDir)
        }
    }

    /** Enqueue one directory entry as a single zip archive. */
    fun enqueueEntryZip(target: SharedFileDto) {
        val (link, urlToken) = transferContext() ?: return
        downloads.enqueueZip(messageId, link, urlToken, listOf(target), "${target.name}.zip")
    }

    /** Enqueue the current multi-selection into [targetDir]; "" = public Downloads. */
    fun enqueueSelection(targetDir: String) {
        val (link, urlToken) = transferContext() ?: return
        if (selected.isEmpty()) return
        downloads.enqueueMulti(messageId, link, urlToken, sortedSelection(), targetDir)
        clearSelection()
    }

    /** Enqueue the current selection as one zip archive into Downloads/PlainApp. */
    fun enqueueSelectionZip() {
        val (link, urlToken) = transferContext() ?: return
        if (selected.isEmpty()) return
        val zipName = (rootInfo?.name ?: "shared") + "_selected.zip"
        downloads.enqueueZip(messageId, link, urlToken, sortedSelection(), zipName)
        clearSelection()
    }

    fun clearPreviewHandles() {
        previewHandles.forEach { deleteFileAt(it) }
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

/** Entries that open like local FilesPage items (preview/play/text/PDF); the rest keep their download sheet. */
private fun SharedFileDto.opensLikeLocal(): Boolean =
    mimeType.startsWith("image/") || mimeType.startsWith("video/") || mimeType.startsWith("audio/") ||
        mimeType == "application/pdf" ||
        name.isImageFast() || name.isVideoFast() || name.isAudioFast() || name.isTextFile() || name.isPdfFile()
