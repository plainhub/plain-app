package com.ismartcoding.plain.ui.shares.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.models.MessageShare
import com.ismartcoding.plain.features.downloads.isTerminalDownloadStatus
import com.ismartcoding.plain.models.File as ShareFile
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** One visited level of the shared folder tree. Root level has an empty [Crumb.path]. */
internal data class Crumb(val path: String, val name: String)

internal class SharedFolderState(
    private val messageId: String,
    private val navController: NavHostController,
    private val scope: CoroutineScope,
    private val previewerState: MediaPreviewerState,
    val source: ContentSource,
    val downloads: com.ismartcoding.plain.features.downloads.DownloadsClient = com.ismartcoding.plain.features.downloads.DownloadCenter,
) {
    var shareMsg by mutableStateOf<MessageShare?>(null)
        private set
    val rootInfo: List<ShareFile>? get() = dirCache[""]
    private var loadingPath by mutableStateOf<String?>(null)
    private var errorPath by mutableStateOf<String?>(null)

    var crumbs by mutableStateOf(listOf<Crumb>())
        private set
    var selectMode by mutableStateOf(false)
    var selected by mutableStateOf(setOf<ShareFile>())
    var downloadTarget by mutableStateOf<ShareFile?>(null)
    var showSaveSelectedSheet by mutableStateOf(false)
    private var previewLoading by mutableStateOf(setOf<String>())

    /** Per-directory content cache: entering a visited directory renders instantly from here. */
    private val dirCache = mutableStateMapOf<String, List<ShareFile>>()

    private val previewHandles = mutableListOf<String>()

    val currentPath: String get() = crumbs.lastOrNull()?.path ?: ""
    val currentInfo: List<ShareFile>? get() = dirCache[currentPath]
    val pathLoading: Boolean get() = loadingPath == currentPath
    val pathError: Boolean get() = errorPath == currentPath
    val entries: List<ShareFile>
        get() = currentInfo?.sortedWith(
            compareBy<ShareFile> { !it.isDir }.thenBy { it.name.lowercase() },
        ) ?: emptyList()

    fun loadMessage() {
        scope.launch {
            try {
                val chat = withIO { source.chats.getChatItem(messageId) }
                shareMsg = chat?.messageContent?.value as? MessageShare
            } catch (canceled: CancellationException) { throw canceled }
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
                source.files.list(path.ifEmpty { msg.browserUrl() }, false, com.ismartcoding.plain.features.files.FileSortBy.NAME_ASC)
            } catch (canceled: CancellationException) { throw canceled }
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
            dirCache[path] = result
            try { shareMsg = withIO { source.chats.getChatItem(messageId) }?.messageContent?.value as? MessageShare }
            catch (canceled: CancellationException) { throw canceled }
            catch (error: Exception) { DialogHelper.showMessage(error) }
        }
    }

    fun onCrumbClick(path: String) {
        if (selectMode) return
        crumbs = if (path.isEmpty()) {
            emptyList()
        } else {
            val index = crumbs.indexOfFirst { it.path == path }
            if (index >= 0) crumbs.take(index + 1) else crumbs
        }
    }

    fun onEntryClick(entry: ShareFile) {
        when {
            selectMode -> selected = if (selected.contains(entry)) selected - entry else selected + entry
            entry.isDir -> crumbs = crumbs + Crumb(entry.path, entry.name)
            entry.opensLikeLocal() -> openEntryPreview(entry)
        }
    }

    fun onEntryLongClick(entry: ShareFile) {
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

    fun isPreviewLoading(entry: ShareFile): Boolean = previewLoading.contains(entry.path)

    /** True while a batch covering [entry] is queued or downloading. */
    fun isEntryBusy(entry: ShareFile): Boolean = downloads.progress.value.values.any {
        !it.status.isTerminalDownloadStatus() &&
            (it.source as? com.ismartcoding.plain.models.ResourceDownloadSource)?.urls?.contains(entry.url) == true
    }

    fun withBrowserUrl(action: (String) -> Unit) {
        val msg = shareMsg ?: return
        scope.launch {
            try {
                action(msg.browserUrl())
            } catch (canceled: CancellationException) { throw canceled }
            catch (_: Exception) { DialogHelper.showErrorDialog(LocaleHelper.getString(Res.string.cannot_load_share)) }
        }
    }

    fun openEntryPreview(entry: ShareFile) {
        if (previewLoading.contains(entry.path)) return
        previewLoading = previewLoading + entry.path
        scope.launch {
            val path = try {
                SharePreview.copy(entry)
            } catch (canceled: CancellationException) { throw canceled }
            catch (error: Exception) {
                DialogHelper.showMessage(error)
                return@launch
            } finally {
                previewLoading = previewLoading - entry.path
            }
            val inPagePreview = !(path.isAudioFast() || path.isTextFile() || path.isPdfFile())
            if (inPagePreview) previewHandles.add(path)
            openFileByType(
                path = path,
                navController = navController,
                source = ContentSource.Local,
                name = entry.name,
                mediaHint = entry.name.isImageFast() || entry.name.isVideoFast(),
                onPreviewMedia = {
                    MediaPreviewData.items = listOf(PreviewItem(id = entry.path, path = path, source = ContentSource.Local, size = entry.size))
                    scope.launch { previewerState.open(0) }
                },
            )
        }
    }

    /** Enqueue one entry save (file streams, dir mirrors) into [targetDir]; "" = public Downloads. */
    fun enqueueEntry(target: ShareFile, targetDir: String) {
        download(listOf(target), targetDir)
    }

    fun enqueueEntryZip(target: ShareFile) {
        download(listOf(target), "", "${target.name}.zip")
    }

    fun enqueueSelection(targetDir: String) {
        if (selected.isEmpty()) return
        if (download(sortedSelection(), targetDir)) clearSelection()
    }

    fun enqueueSelectionZip() {
        if (selected.isEmpty()) return
        if (download(sortedSelection(), "", (shareMsg?.name ?: "shared") + "_selected.zip")) clearSelection()
    }

    private fun download(entries: List<ShareFile>, targetDir: String, zipName: String? = null): Boolean =
        downloads.create(com.ismartcoding.plain.models.DownloadInput(resources = com.ismartcoding.plain.models.ResourceDownloadInput(entries.map { it.url }, targetDir, zipName)))

    fun clearPreviewHandles() {
        previewHandles.forEach { deleteFileAt(it) }
        previewHandles.clear()
    }

    private fun sortedSelection(): List<ShareFile> =
        selected.sortedWith(compareBy<ShareFile> { !it.isDir }.thenBy { it.name.lowercase() })
}

/** Entries that open like local FilesPage items (preview/play/text/PDF); the rest keep their download sheet. */
private fun ShareFile.opensLikeLocal(): Boolean =
    name.isImageFast() || name.isVideoFast() || name.isAudioFast() || name.isTextFile() || name.isPdfFile()
