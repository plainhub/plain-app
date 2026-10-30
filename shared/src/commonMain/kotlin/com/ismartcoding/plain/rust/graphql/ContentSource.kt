package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.files.FavoriteFoldersApi
import com.ismartcoding.plain.features.files.FavoriteFoldersClient
import com.ismartcoding.plain.features.files.FileLocationApi
import com.ismartcoding.plain.features.files.FileLocationClient
import com.ismartcoding.plain.features.files.FileTaskApi
import com.ismartcoding.plain.features.files.FileTaskClient
import com.ismartcoding.plain.features.files.FilesApi
import com.ismartcoding.plain.features.files.FilesClient
import com.ismartcoding.plain.features.shares.SharesApi
import com.ismartcoding.plain.features.shares.SharesClient
import com.ismartcoding.plain.models.ManagedDeviceInfo
import com.ismartcoding.plain.platform.files.fileExists
import com.ismartcoding.plain.platform.files.getInternalStoragePath
import com.ismartcoding.plain.platform.system.appDir

sealed class ContentSource(val api: GraphqlClient) {
    object Local : ContentSource(GraphqlApi)
    class Remote internal constructor(internal val client: ManagedDeviceClient) : ContentSource(client.api) {
        val info: ManagedDeviceInfo get() = client.currentInfo
    }

    val key: String get() = when (this) { Local -> "local"; is Remote -> client.key }

    internal val cast by lazy { when (this) {
        Local -> com.ismartcoding.plain.features.dlna.sender.RustDlnaSender
        is Remote -> com.ismartcoding.plain.features.dlna.sender.DlnaSenderClient(api)
    } }

    internal val audioPlayback by lazy { when (this) { Local -> null; is Remote -> com.ismartcoding.plain.features.audio.RemoteAudioPlayback(this) } }

    val capabilities: Collection<com.ismartcoding.plain.platform.system.Capability> get() = when (this) {
        Local -> com.ismartcoding.plain.platform.system.getDeviceCapabilities()
        is Remote -> info.capabilities
    }

    val catalog by lazy {
        if (this is Remote) com.ismartcoding.plain.features.media.MediaCatalogApi(api) { _, _, path -> fileForOpen(path) }
        else com.ismartcoding.plain.features.media.MediaCatalogClient
    }
    val media by lazy { com.ismartcoding.plain.features.media.MediaApi(api) }
    val mediaActions by lazy { com.ismartcoding.plain.features.media.actions.MediaActionApi(api) }
    val chats by lazy { com.ismartcoding.plain.features.chat.ChatApi(api) }
    val chatSender by lazy { com.ismartcoding.plain.features.chat.ShareSendApi(api) }
    val storedFiles by lazy { com.ismartcoding.plain.features.files.stored.AppFilesApi(api) }
    val packages by lazy { com.ismartcoding.plain.features.packages.PackagesApi(api) }
    internal val tags by lazy { com.ismartcoding.plain.features.tags.TagsApi(api) }
    internal val notes by lazy { com.ismartcoding.plain.features.notes.NotesApi(api) }
    internal val feeds by lazy { com.ismartcoding.plain.features.feeds.FeedsApi(api) }
    val feedEntries by lazy { com.ismartcoding.plain.features.feeds.FeedEntriesApi(api) }
    val queue by lazy { if (this is Remote) com.ismartcoding.plain.features.audio.AudioQueueApi(api) else com.ismartcoding.plain.features.audio.AudioQueueClient }
    val playlists by lazy { com.ismartcoding.plain.features.audio.AudioPlaylistsApi(api) }
    val audioOverview by lazy { com.ismartcoding.plain.features.audio.AudioOverviewApi(api) }
    val history by lazy { com.ismartcoding.plain.features.media.MediaPlayHistoryApi(api) }
    val files: FilesApi by lazy { when (this) {
        Local -> FilesClient
        is Remote -> FilesApi(api, client::writeChunks)
    } }
    val tasks: FileTaskApi by lazy { if (this is Remote) FileTaskApi(api) else FileTaskClient }
    val favorites: FavoriteFoldersApi by lazy { if (this is Remote) FavoriteFoldersApi(api) else FavoriteFoldersClient }
    val locations: FileLocationApi by lazy { if (this is Remote) FileLocationApi(api) else FileLocationClient }
    val sharedLinks by lazy { if (this is Remote) com.ismartcoding.plain.features.shares.SharedLinkApi(api) else com.ismartcoding.plain.features.shares.SharedLinkClient }
    val shares: SharesApi by lazy { if (this is Remote) SharesApi(api) else SharesClient }
    val appPath: String get() = when (this) {
        Local -> appDir()
        is Remote -> info.appDir
    }
    val rootPath: String get() = when (this) {
        Local -> getInternalStoragePath()
        is Remote -> info.appDir
    }

    suspend fun fileForOpen(path: String, name: String = path.substringAfterLast('/')): String = when (this) {
        Local -> path
        is Remote -> client.downloadFile(path, name)
    }

    suspend fun exists(path: String): Boolean = when (this) {
        Local -> fileExists(path)
        is Remote -> if (path == info.appDir || path == info.downloadsDir) true else {
            val parent = path.trimEnd('/').substringBeforeLast('/', "/")
            files.searchName(path.trimEnd('/').substringAfterLast('/'), parent, true, com.ismartcoding.plain.features.files.FileSortBy.NAME_ASC).any { it.path == path }
        }
    }
}
