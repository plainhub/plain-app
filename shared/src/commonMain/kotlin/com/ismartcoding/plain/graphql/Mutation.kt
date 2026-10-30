package com.ismartcoding.plain.graphql

import com.ismartcoding.plain.features.files.FileSortBy

import com.ismartcoding.plain.models.AdvancedAudioSelection
import com.ismartcoding.plain.models.MergeResult
import com.ismartcoding.plain.features.audio.AudioSelectionAction
import com.ismartcoding.plain.features.audio.MediaPlayMode
import com.ismartcoding.plain.features.chat.download.ChatDownloadCommand
import com.ismartcoding.plain.features.chat.models.MessageShare
import com.ismartcoding.plain.features.dlna.sender.CastCommand
import com.ismartcoding.plain.features.dlna.sender.CastSnapshot
import com.ismartcoding.plain.features.files.FileTaskOp
import com.ismartcoding.plain.features.files.FileTaskType
import com.ismartcoding.plain.features.files.FileWriteAction
import com.ismartcoding.plain.features.media.actions.MediaAction
import com.ismartcoding.plain.features.nearby.PairingState
import com.ismartcoding.plain.features.shares.SharedBrowseResult
import com.ismartcoding.plain.features.shares.SharedDownloadCommand
import com.ismartcoding.plain.features.shares.SharedDownloadInput
import com.ismartcoding.plain.features.shares.SharedFileDto
import com.ismartcoding.plain.features.shares.SharedLink
import com.ismartcoding.plain.models.ActionResult
import com.ismartcoding.plain.models.AppFile
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.models.AudioPlaylist
import com.ismartcoding.plain.models.ChatChannel
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.models.ChatTextEdit
import com.ismartcoding.plain.models.FavoriteFolder
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.models.File as FileEntry
import com.ismartcoding.plain.models.FileTask
import com.ismartcoding.plain.models.ImageEditorProject
import com.ismartcoding.plain.models.ImageEditorProjectInput
import com.ismartcoding.plain.models.Note
import com.ismartcoding.plain.models.NoteInput
import com.ismartcoding.plain.models.PairingDeviceInput
import com.ismartcoding.plain.models.PairingRequestInput
import com.ismartcoding.plain.models.PomodoroToday
import com.ismartcoding.plain.models.ShareRecord
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.variable
import kotlin.time.Instant
import kotlinx.serialization.json.JsonElement

object Mutation {
    fun uninstallPackages(ids: List<String>): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($ids: [ID!]!) { uninstallPackages(ids: $ids) }",
        mapOf(variable("ids", ids)), field = "uninstallPackages",
    )

    fun openWebSettings(feature: com.ismartcoding.plain.features.web.WebSettingsFeature?): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($feature: WebSettingsFeature) { openWebSettings(feature: $feature) }",
        mapOf(variable("feature", feature?.name)), field = "openWebSettings",
    )

    fun editChatText(id: String, text: String): GraphqlRequest<ChatTextEdit> = GraphqlRequest(
        $$"mutation($id: ID!, $text: String!) { editChatText(id: $id, text: $text) { changed chat { $${Fragments.CHAT_ITEM} } } }",
        mapOf(variable("id", id), variable("text", text)),
        field = "editChatText",
    )

    fun sendChatItem(target: String, content: String): GraphqlRequest<List<ChatItem>> = GraphqlRequest(
        $$"mutation($target: String!, $content: String!) { sendChatItem(target: $target, content: $content) { $${Fragments.CHAT_ITEM} } }",
        mapOf(variable("target", target), variable("content", content)),
        field = "sendChatItem",
    )

    fun forwardChatItems(id: String, targets: Collection<String>): GraphqlRequest<List<ChatItem>> = GraphqlRequest(
        $$"mutation($id: ID!, $targets: [String!]!) { forwardChatItems(id: $id, targets: $targets) { $${Fragments.CHAT_ITEM} } }",
        mapOf(variable("id", id), variable("targets", targets)),
        field = "forwardChatItems",
    )

    fun deleteChatItems(query: String): GraphqlRequest<ActionResult> = GraphqlRequest(
        $$"mutation($query: String!) { deleteChatItems(query: $query) { affectedCount } }",
        mapOf(variable("query", query)),
        field = "deleteChatItems",
    )

    fun deleteChatItem(id: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!) { deleteChatItem(id: $id) }",
        mapOf(variable("id", id)),
        field = "deleteChatItem",
    )

    fun pairDevice(input: PairingDeviceInput): GraphqlRequest<PairingState> = GraphqlRequest(
        $$"mutation($input: PairingDeviceInput!) { pairDevice(input: $input) { $${Fragments.PAIRING_STATE} } }",
        mapOf(variable("input", input)),
        field = "pairDevice",
    )

    fun cancelPairing(generation: String): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($generation: String!) { cancelPairing(generation: $generation) }",
        mapOf(variable("generation", generation)),
        field = "cancelPairing",
    )

    fun respondToPairing(input: PairingRequestInput, accepted: Boolean): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($input: PairingRequestInput!, $accepted: Boolean!) { respondToPairing(input: $input, accepted: $accepted) }",
        mapOf(variable("input", input), variable("accepted", accepted)),
        field = "respondToPairing",
    )

    fun acquireDiscovery(token: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($token: ID!) { acquireDiscovery(token: $token) }",
        mapOf(variable("token", token)),
        field = "acquireDiscovery",
    )

    fun releaseDiscovery(token: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($token: ID!) { releaseDiscovery(token: $token) }",
        mapOf(variable("token", token)),
        field = "releaseDiscovery",
    )

    fun createTag(type: DataType, name: String): GraphqlRequest<Tag> = GraphqlRequest(
        $$"mutation($type: DataType!, $name: String!) { createTag(type: $type, name: $name) { $${Fragments.TAG_FIELDS} } }",
        mapOf(variable("type", type.name), variable("name", name)),
        field = "createTag",
    )

    fun updateTag(id: String, name: String): GraphqlRequest<Tag> = GraphqlRequest(
        $$"mutation($id: ID!, $name: String!) { updateTag(id: $id, name: $name) { $${Fragments.TAG_FIELDS} } }",
        mapOf(variable("id", id), variable("name", name)),
        field = "updateTag",
    )

    fun deleteTag(id: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!) { deleteTag(id: $id) }",
        mapOf(variable("id", id)),
        field = "deleteTag",
    )

    fun updateTagRelations(
        type: DataType,
        keys: Collection<String>,
        addTagIds: Collection<String>,
        removeTagIds: Collection<String>,
    ): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($type: DataType!, $keys: [String!]!, $addTagIds: [ID!]!, $removeTagIds: [ID!]!) { updateTagRelations(type: $type, keys: $keys, addTagIds: $addTagIds, removeTagIds: $removeTagIds) }",
        mapOf(variable("type", type.name), variable("keys", keys), variable("addTagIds", addTagIds), variable("removeTagIds", removeTagIds)),
        field = "updateTagRelations",
    )

    fun markFeedEntriesRead(query: String, read: Boolean): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($query: String!, $read: Boolean!) { markFeedEntriesRead(query: $query, read: $read) { affectedCount } }",
        mapOf(variable("query", query), variable("read", read)),
        field = "markFeedEntriesRead",
    )

    fun deleteFeedEntries(query: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($query: String!) { deleteFeedEntries(query: $query) { affectedCount } }",
        mapOf(variable("query", query)),
        field = "deleteFeedEntries",
    )

    fun createFeed(url: String, name: String, fetchContent: Boolean): GraphqlRequest<Feed> = GraphqlRequest(
        $$"mutation($url: String!, $name: String, $fetchContent: Boolean!) { createFeed(url: $url, name: $name, fetchContent: $fetchContent) { $${Fragments.FEED_FIELDS} } }",
        mapOf(variable("url", url), variable("name", name), variable("fetchContent", fetchContent)),
        field = "createFeed",
    )

    fun deleteFeed(id: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!) { deleteFeed(id: $id) }",
        mapOf(variable("id", id)),
        field = "deleteFeed",
    )

    fun importFeeds(content: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($content: String!) { importFeeds(content: $content) }",
        mapOf(variable("content", content)),
        field = "importFeeds",
    )

    fun exportFeeds(): GraphqlRequest<String> = GraphqlRequest(
        $$"mutation { exportFeeds }",
        field = "exportFeeds",
    )

    fun saveMediaPlayProgress(mediaId: String, type: DataType, positionMs: Long): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($mediaId: ID!, $type: MediaDataType!, $positionMs: Long!) { saveMediaPlayProgress(mediaId: $mediaId, type: $type, positionMs: $positionMs) }",
        mapOf(variable("mediaId", mediaId), variable("type", type.name), variable("positionMs", positionMs)),
        field = "saveMediaPlayProgress",
    )

    fun recordMediaPlay(mediaId: String, type: DataType, positionMs: Long): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($mediaId: ID!, $type: MediaDataType!, $positionMs: Long!) { recordMediaPlay(mediaId: $mediaId, type: $type, positionMs: $positionMs) }",
        mapOf(variable("mediaId", mediaId), variable("type", type.name), variable("positionMs", positionMs)),
        field = "recordMediaPlay",
    )

    fun createAudioPlaylist(name: String): GraphqlRequest<AudioPlaylist> = GraphqlRequest(
        $$"mutation($name: String!) { createAudioPlaylist(name: $name) { $${Fragments.AUDIO_PLAYLIST_FIELDS} } }",
        mapOf(variable("name", name)),
        field = "createAudioPlaylist",
    )

    fun updateAudioPlaylist(id: String, name: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!, $name: String!) { updateAudioPlaylist(id: $id, name: $name) { id } }",
        mapOf(variable("id", id), variable("name", name)),
        field = "updateAudioPlaylist",
    )

    fun deleteAudioPlaylist(id: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!) { deleteAudioPlaylist(id: $id) }",
        mapOf(variable("id", id)),
        field = "deleteAudioPlaylist",
    )

    fun addAudioPlaylistItems(id: String, query: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!, $query: String!) { addAudioPlaylistItems(id: $id, query: $query) }",
        mapOf(variable("id", id), variable("query", query)),
        field = "addAudioPlaylistItems",
    )

    fun removeAudioPlaylistItem(id: String, query: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!, $query: String!) { removeAudioPlaylistItem(id: $id, query: $query) { affectedCount } }",
        mapOf(variable("id", id), variable("query", query)),
        field = "removeAudioPlaylistItem",
    )

    internal fun enqueueAndSelectAudio(mediaId: String): GraphqlRequest<AudioItem> = GraphqlRequest(
        $$"mutation($query: String!, $mediaId: ID!) { addAudioToQueue(query: $query) playAudio(mediaId: $mediaId) { $${Fragments.AUDIO_TRACK_FIELDS} } }",
        mapOf(variable("query", com.ismartcoding.plain.rust.graphql.selectionQuery(setOf(mediaId))), variable("mediaId", mediaId)),
        field = "playAudio",
    )

    fun playAudio(mediaId: String): GraphqlRequest<AudioItem> = GraphqlRequest(
        $$"mutation($mediaId: ID!) { playAudio(mediaId: $mediaId) { $${Fragments.AUDIO_TRACK_FIELDS} } }",
        mapOf(variable("mediaId", mediaId)), field = "playAudio",
    )

    fun advanceAudioSelection(
        action: AudioSelectionAction,
        expectedPath: String,
        expectedRevision: Long,
        shuffle: Boolean,
    ): GraphqlRequest<AdvancedAudioSelection?> = GraphqlRequest(
        $$"mutation($action: AudioSelectionAction!, $expectedPath: String!, $expectedRevision: Long!, $shuffle: Boolean!) { advanceAudioSelection(action: $action, expectedPath: $expectedPath, expectedRevision: $expectedRevision, shuffle: $shuffle) { path positionMs revision track { $${Fragments.AUDIO_TRACK_FIELDS} } } }",
        mapOf(variable("action", action.name), variable("expectedPath", expectedPath), variable("expectedRevision", expectedRevision), variable("shuffle", shuffle)),
        field = "advanceAudioSelection",
    )

    fun replaceAudioQueue(query: String, sortBy: FileSortBy, shuffle: Boolean): GraphqlRequest<AudioItem?> = GraphqlRequest(
        $$"mutation($query: String!, $sortBy: FileSortBy!, $shuffle: Boolean!) { replaceAudioQueue(query: $query, sortBy: $sortBy, shuffle: $shuffle) { $${Fragments.AUDIO_TRACK_FIELDS} } }",
        mapOf(variable("query", query), variable("sortBy", sortBy.name), variable("shuffle", shuffle)),
        field = "replaceAudioQueue",
    )

    fun clearAudioQueue(): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation { clearAudioQueue }",
        field = "clearAudioQueue",
    )

    fun addAudioToQueue(query: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($query: String!) { addAudioToQueue(query: $query) }",
        mapOf(variable("query", query)),
        field = "addAudioToQueue",
    )

    fun removeAudioFromQueue(mediaId: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($mediaId: ID!) { removeAudioFromQueue(mediaId: $mediaId) }",
        mapOf(variable("mediaId", mediaId)),
        field = "removeAudioFromQueue",
    )

    fun reorderAudioQueue(mediaIds: Collection<String>): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($mediaIds: [ID!]!) { reorderAudioQueue(mediaIds: $mediaIds) }",
        mapOf(variable("mediaIds", mediaIds)),
        field = "reorderAudioQueue",
    )

    fun audioCommand(action: AudioCommand, positionMs: Long, speed: Float): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($action: AudioCommandAction!, $positionMs: Long!, $speed: Float!) { audioCommand(action: $action, positionMs: $positionMs, speed: $speed) { path positionMs revision } }",
        mapOf(variable("action", action.name), variable("positionMs", positionMs), variable("speed", speed)),
        field = "audioCommand",
    )

    fun updateAudioPlayMode(mode: MediaPlayMode): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($mode: MediaPlayMode!) { updateAudioPlayMode(mode: $mode) }",
        mapOf(variable("mode", mode.name)),
        field = "updateAudioPlayMode",
    )

    fun audioPlayTrack(mediaId: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($mediaId: ID!) { audioPlayTrack(mediaId: $mediaId) { path positionMs revision } }",
        mapOf(variable("mediaId", mediaId)),
        field = "audioPlayTrack",
    )

    fun audioPlayPath(path: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($path: String!) { audioPlayPath(path: $path) { path positionMs revision } }",
        mapOf(variable("path", path)),
        field = "audioPlayPath",
    )

    fun renameSession(clientId: String, name: String): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($clientId: String!, $name: String!) { renameSession(clientId: $clientId, name: $name) }",
        mapOf(variable("clientId", clientId), variable("name", name)),
        field = "renameSession",
    )

    fun revokeSession(clientId: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($clientId: String!) { revokeSession(clientId: $clientId) }",
        mapOf(variable("clientId", clientId)),
        field = "revokeSession",
    )

    fun saveFeedEntriesToNotes(query: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($query: String!) { saveFeedEntriesToNotes(query: $query) }",
        mapOf(variable("query", query)),
        field = "saveFeedEntriesToNotes",
    )

    fun trashNotes(query: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($query: String!) { trashNotes(query: $query) { affectedCount } }",
        mapOf(variable("query", query)),
        field = "trashNotes",
    )

    fun restoreNotes(query: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($query: String!) { restoreNotes(query: $query) { affectedCount } }",
        mapOf(variable("query", query)),
        field = "restoreNotes",
    )

    fun deleteNotes(query: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($query: String!) { deleteNotes(query: $query) { affectedCount } }",
        mapOf(variable("query", query)),
        field = "deleteNotes",
    )

    fun startPomodoro(durationSec: Int): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($durationSec: Int!) { startPomodoro(durationSec: $durationSec) }",
        mapOf(variable("durationSec", durationSec)),
        field = "startPomodoro",
    )

    fun pausePomodoro(): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation { pausePomodoro }",
        field = "pausePomodoro",
    )

    fun stopPomodoro(): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation { stopPomodoro }",
        field = "stopPomodoro",
    )

    fun adjustPomodoro(durationSec: Int): GraphqlRequest<PomodoroToday> = GraphqlRequest(
        $$"mutation($durationSec: Int!) { adjustPomodoro(durationSec: $durationSec) { $${Fragments.POMODORO} } }",
        mapOf(variable("durationSec", durationSec)),
        field = "adjustPomodoro",
    )

    fun copyFile(src: String, dst: String, overwrite: Boolean): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($src: String!, $dst: String!, $overwrite: Boolean!) { copyFile(src: $src, dst: $dst, overwrite: $overwrite) }",
        mapOf(variable("src", src), variable("dst", dst), variable("overwrite", overwrite)),
        field = "copyFile",
    )

    fun copyFileToDirectory(src: String, directory: String, name: String): GraphqlRequest<String?> = GraphqlRequest(
        $$"mutation($src:String!,$directory:String!,$name:String!){copyFileToDirectory(src:$src,directory:$directory,name:$name)}",
        mapOf(variable("src", src), variable("directory", directory), variable("name", name)),
        field = "copyFileToDirectory",
    )

    fun extractZipArchive(path: String, target: String): GraphqlRequest<String> = GraphqlRequest(
        $$"mutation($path: String!, $target: String!) { extractZipArchive(path: $path, target: $target) }",
        mapOf(variable("path", path), variable("target", target)),
        field = "extractZipArchive",
    )

    fun cacheZipEntry(path: String): GraphqlRequest<String?> = GraphqlRequest(
        $$"mutation($path: String!) { cacheZipEntry(path: $path) }",
        mapOf(variable("path", path)),
        field = "cacheZipEntry",
    )

    fun renameFile(path: String, name: String): GraphqlRequest<String?> = GraphqlRequest(
        $$"mutation($path: String!, $name: String!) { renameFile(path: $path, name: $name) }",
        mapOf(variable("path", path), variable("name", name)),
        field = "renameFile",
    )

    fun deleteFiles(paths: Collection<String>): GraphqlRequest<ActionResult> = GraphqlRequest(
        $$"mutation($paths: [String!]!) { deleteFiles(paths: $paths) { affectedCount } }",
        mapOf(variable("paths", paths)),
        field = "deleteFiles",
    )

    fun addFavoriteFolder(rootPath: String, fullPath: String): GraphqlRequest<List<FavoriteFolder>> = GraphqlRequest(
        $$"mutation($rootPath: String!, $fullPath: String!) { addFavoriteFolder(rootPath: $rootPath, fullPath: $fullPath) { $${Fragments.FAVORITE_FOLDER} } }",
        mapOf(variable("rootPath", rootPath), variable("fullPath", fullPath)),
        field = "addFavoriteFolder",
    )

    fun removeFavoriteFolder(fullPath: String): GraphqlRequest<List<FavoriteFolder>> = GraphqlRequest(
        $$"mutation($fullPath: String!) { removeFavoriteFolder(fullPath: $fullPath) { $${Fragments.FAVORITE_FOLDER} } }",
        mapOf(variable("fullPath", fullPath)),
        field = "removeFavoriteFolder",
    )

    fun setFavoriteFolderAlias(fullPath: String, alias: String): GraphqlRequest<List<FavoriteFolder>> = GraphqlRequest(
        $$"mutation($fullPath: String!, $alias: String!) { setFavoriteFolderAlias(fullPath: $fullPath, alias: $alias) { $${Fragments.FAVORITE_FOLDER} } }",
        mapOf(variable("fullPath", fullPath), variable("alias", alias)),
        field = "setFavoriteFolderAlias",
    )

    fun createFileTask(type: FileTaskType, ops: List<FileTaskOp>): GraphqlRequest<FileTask> = GraphqlRequest(
        $$"mutation($type:FileTaskType!,$title:String!,$ops:[FileTaskOpInput!]!){createFileTask(type:$type,title:$title,ops:$ops,waitForCompletion:true){ $${Fragments.FILE_TASK} }}",
        mapOf(variable("type", type.name), variable("title", type.name), variable("ops", ops)),
        field = "createFileTask",
    )

    fun browseSharedLink(
        messageId: String,
        expected: MessageShare,
        virtualPath: String?,
    ): GraphqlRequest<SharedBrowseResult?> = GraphqlRequest(
        $$"mutation($messageId: ID!, $expected: SharedMessageCardInput!, $virtualPath: String) { browseSharedLink(messageId: $messageId, expected: $expected, virtualPath: $virtualPath) { info { $${Fragments.SHARED_INFO} } link { $${Fragments.SHARE_LINK} } card { $${Fragments.SHARED_CARD} } } }",
        mapOf(variable("messageId", messageId), variable("expected", expected), variable("virtualPath", virtualPath)),
        field = "browseSharedLink",
    )

    fun cacheSharedFile(
        link: SharedLink,
        urlToken: String,
        entry: SharedFileDto,
        thumbnail: Boolean,
    ): GraphqlRequest<String> = GraphqlRequest(
        $$"mutation($link: SharedLinkInput!, $urlToken: String!, $entry: SharedEntryInput!, $thumbnail: Boolean!) { cacheSharedFile(link: $link, urlToken: $urlToken, entry: $entry, thumbnail: $thumbnail) }",
        mapOf(variable("link", link), variable("urlToken", urlToken), variable("entry", entry), variable("thumbnail", thumbnail)),
        field = "cacheSharedFile",
    )

    fun deleteShare(id: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!) { deleteShare(id: $id) }",
        mapOf(variable("id", id)),
        field = "deleteShare",
    )

    internal fun enqueueSharedDownload(input: SharedDownloadInput): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($input: SharedDownloadInput!) { enqueueSharedDownload(input: $input) }",
        mapOf(variable("input", input)),
        field = "enqueueSharedDownload",
    )

    fun cacheAudioCover(path: String): GraphqlRequest<String?> = GraphqlRequest(
        $$"mutation CacheAudioCover($path: String!) { cacheAudioCover(path: $path) }",
        mapOf(variable("path", path)),
        field = "cacheAudioCover",
    )

    fun deleteClipboardItems(query: String): GraphqlRequest<ActionResult> = GraphqlRequest(
        $$"mutation($query: String!) { deleteClipboardItems(query: $query) { affectedCount } }",
        mapOf(variable("query", query)),
        field = "deleteClipboardItems",
    )

    fun mergeChunks(fileId: String, totalChunks: Long, path: String, totalSize: Long, replace: Boolean = true): GraphqlRequest<MergeResult> = GraphqlRequest(
        $$"mutation($fileId: String!, $totalChunks: Int!, $path: String!, $totalSize: Long!, $replace: Boolean!) { mergeChunks(fileId: $fileId, totalChunks: $totalChunks, path: $path, replace: $replace, totalSize: $totalSize) { status value error } }",
        mapOf(variable("fileId", fileId), variable("totalChunks", totalChunks), variable("path", path), variable("totalSize", totalSize), variable("replace", replace)),
        field = "mergeChunks",
    )

    fun deleteChunks(fileId: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($fileId: String!) { deleteChunks(fileId: $fileId) }",
        mapOf(variable("fileId", fileId)),
        field = "deleteChunks",
    )

    fun importAppFile(
        source: String,
        fileName: String,
        mimeType: String,
        deleteSource: Boolean,
    ): GraphqlRequest<AppFile> = GraphqlRequest(
        $$"mutation($source: String!, $fileName: String!, $mimeType: String!, $deleteSource: Boolean!) { importAppFile(source: $source, fileName: $fileName, mimeType: $mimeType, deleteSource: $deleteSource) { $${Fragments.APP_FILE} } }",
        mapOf(variable("source", source), variable("fileName", fileName), variable("mimeType", mimeType), variable("deleteSource", deleteSource)),
        field = "importAppFile",
    )

    fun releaseAppFile(fileId: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($fileId: String!) { releaseAppFile(fileId: $fileId) }",
        mapOf(variable("fileId", fileId)),
        field = "releaseAppFile",
    )

    fun saveImageEditorProject(id: String, project: ImageEditorProject): GraphqlRequest<ImageEditorProject> {
        val input = ImageEditorProjectInput(project.stateB64, project.thumbnail, project.canvasWidth, project.canvasHeight, project.layerCount)
        return GraphqlRequest(
            $$"mutation($id: ID!, $input: ImageEditorProjectInput!) { saveImageEditorProject(id: $id, input: $input) { $${Fragments.IMAGE_EDITOR_PROJECT} } }",
            mapOf(variable("id", id), variable("input", input)),
            field = "saveImageEditorProject",
        )
    }

    fun deleteImageEditorProject(id: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!) { deleteImageEditorProject(id: $id) }",
        mapOf(variable("id", id)),
        field = "deleteImageEditorProject",
    )

    fun deletePeer(id: String): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($id: ID!) { deletePeer(id: $id) }",
        mapOf(variable("id", id)),
        field = "deletePeer",
    )

    fun unpairPeer(id: String): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($id: ID!) { unpairPeer(id: $id) }",
        mapOf(variable("id", id)),
        field = "unpairPeer",
    )

    fun createChatChannel(name: String): GraphqlRequest<ChatChannel> = GraphqlRequest(
        $$"mutation($name: String!) { createChatChannel(name: $name) { $${Fragments.CHANNEL_FIELDS} } }",
        mapOf(variable("name", name)),
        field = "createChatChannel",
    )

    fun updateChatChannel(id: String, name: String): GraphqlRequest<ChatChannel> = GraphqlRequest(
        $$"mutation($id: ID!, $name: String!) { updateChatChannel(id: $id, name: $name) { $${Fragments.CHANNEL_FIELDS} } }",
        mapOf(variable("id", id), variable("name", name)),
        field = "updateChatChannel",
    )

    fun deleteChatChannel(id: String): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($id: ID!) { deleteChatChannel(id: $id) }",
        mapOf(variable("id", id)),
        field = "deleteChatChannel",
    )

    fun leaveChatChannel(id: String): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($id: ID!) { leaveChatChannel(id: $id) }",
        mapOf(variable("id", id)),
        field = "leaveChatChannel",
    )

    fun addChatChannelMember(id: String, peerId: String): GraphqlRequest<ChatChannel> = GraphqlRequest(
        $$"mutation($id: ID!, $peerId: ID!) { addChatChannelMember(id: $id, peerId: $peerId) { $${Fragments.CHANNEL_FIELDS} } }",
        mapOf(variable("id", id), variable("peerId", peerId)),
        field = "addChatChannelMember",
    )

    fun removeChatChannelMember(id: String, peerId: String): GraphqlRequest<ChatChannel> = GraphqlRequest(
        $$"mutation($id: ID!, $peerId: ID!) { removeChatChannelMember(id: $id, peerId: $peerId) { $${Fragments.CHANNEL_FIELDS} } }",
        mapOf(variable("id", id), variable("peerId", peerId)),
        field = "removeChatChannelMember",
    )

    fun acceptChatChannelInvite(id: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID!) { acceptChatChannelInvite(id: $id) }",
        mapOf(variable("id", id)),
        field = "acceptChatChannelInvite",
    )

    fun declineChatChannelInvite(id: String): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($id: ID!) { declineChatChannelInvite(id: $id) }",
        mapOf(variable("id", id)),
        field = "declineChatChannelInvite",
    )

    fun clearAppLogs(): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation { clearAppLogs }",
        field = "clearAppLogs",
    )

    fun exportAppLogs(): GraphqlRequest<String?> = GraphqlRequest(
        $$"mutation { exportAppLogs }",
        field = "exportAppLogs",
    )

    fun clearAppCache(): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation { clearAppCache }",
        field = "clearAppCache",
    )

    fun deliverChatItem(id: String, recipients: Collection<String>?): GraphqlRequest<ChatItem> = GraphqlRequest(
        $$"mutation($id: ID!, $recipients: [ID!]) { deliverChatItem(id: $id, recipients: $recipients) { $${Fragments.CHAT_ITEM} } }",
        mapOf(variable("id", id), variable("recipients", recipients)),
        field = "deliverChatItem",
    )

    fun sendChatItems(targets: Collection<String>, content: String): GraphqlRequest<List<ChatItem>> = GraphqlRequest(
        $$"mutation($targets: [String!]!, $content: String!) { sendChatItems(targets: $targets, content: $content) { $${Fragments.CHAT_ITEM} } }",
        mapOf(variable("targets", targets), variable("content", content)),
        field = "sendChatItems",
    )

    fun sendShare(targets: Collection<String>, shareId: String): GraphqlRequest<List<ChatItem>> = GraphqlRequest(
        $$"mutation($targets: [String!]!, $shareId: ID!) { sendShare(targets: $targets, shareId: $shareId) { $${Fragments.CHAT_ITEM} } }",
        mapOf(variable("targets", targets), variable("shareId", shareId)),
        field = "sendShare",
    )

    fun sendFolderShare(
        targets: Collection<String>,
        path: String,
        name: String,
        expiresAt: Instant?,
    ): GraphqlRequest<List<ChatItem>> = GraphqlRequest(
        $$"mutation($targets: [String!]!, $path: String!, $name: String!, $expiresAt: Instant) { sendFolderShare(targets: $targets, path: $path, name: $name, expiresAt: $expiresAt) { $${Fragments.CHAT_ITEM} } }",
        mapOf(variable("targets", targets), variable("path", path), variable("name", name), variable("expiresAt", expiresAt)),
        field = "sendFolderShare",
    )

    fun updateFeed(id: String, name: String?, url: String?, fetchContent: Boolean?): GraphqlRequest<Feed> = GraphqlRequest(
        $$"mutation($id: ID!, $name: String, $url: String, $fetchContent: Boolean) { updateFeed(id: $id, name: $name, url: $url, fetchContent: $fetchContent) { $${Fragments.FEED_FIELDS} } }",
        mapOf(variable("id", id)) + listOfNotNull(
            name?.let { variable("name", it) },
            url?.let { variable("url", it) },
            fetchContent?.let { variable("fetchContent", it) },
        ),
        field = "updateFeed",
    )

    fun syncFeeds(id: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($id: ID) { syncFeeds(id: $id) }",
        mapOf(variable("id", id.ifEmpty { null })),
        field = "syncFeeds",
    )

    fun saveNote(id: String, title: String, content: String): GraphqlRequest<Note> {
        val input = NoteInput(title, content)
        return if (id.isEmpty()) {
            GraphqlRequest(
                $$"mutation($input: NoteInput!) { createNote(input: $input) { $${Fragments.NOTE_FIELDS} } }",
                mapOf(variable("input", input)),
                field = "createNote",
            )
        } else {
            GraphqlRequest(
                $$"mutation($id: ID!, $input: NoteInput!) { updateNote(id: $id, input: $input) { $${Fragments.NOTE_FIELDS} } }",
                mapOf(variable("id", id), variable("input", input)),
                field = "updateNote",
            )
        }
    }

    fun createZipArchive(paths: Collection<String>, target: String): GraphqlRequest<String> = GraphqlRequest(
        $$"mutation($paths: [String!]!, $target: String!) { createZipArchive(paths: $paths, target: $target) }",
        mapOf(variable("paths", paths), variable("target", target)),
        field = "createZipArchive",
    )

    fun fileWrite(
        action: FileWriteAction,
        target: String,
        content: String,
        overwrite: Boolean,
    ): GraphqlRequest<FileEntry> = when (action) {
        FileWriteAction.CREATE_DIRECTORY -> GraphqlRequest(
            $$"mutation($path: String!) { createDir(path: $path) { $${Fragments.FILE} } }",
            mapOf(variable("path", target)),
            field = "createDir",
        )
        FileWriteAction.CREATE_FILE -> GraphqlRequest(
            $$"mutation($path: String!) { createFile(path: $path) { $${Fragments.FILE} } }",
            mapOf(variable("path", target)),
            field = "createFile",
        )
        FileWriteAction.WRITE_TEXT -> GraphqlRequest(
            $$"mutation($path: String!, $content: String!, $overwrite: Boolean!) { writeTextFile(path: $path, content: $content, overwrite: $overwrite) { $${Fragments.FILE} } }",
            mapOf(variable("path", target), variable("content", content), variable("overwrite", overwrite)),
            field = "writeTextFile",
        )
    }

    fun createShare(
        name: String,
        realPaths: Collection<String>,
        readOnly: Boolean,
        expiresAt: Instant?,
    ): GraphqlRequest<ShareRecord> = GraphqlRequest(
        $$"mutation($name: String!, $realPaths: [String!]!, $readOnly: Boolean!, $expiresAt: Instant) { createShare(name: $name, realPaths: $realPaths, readOnly: $readOnly, expiresAt: $expiresAt) { $${Fragments.SHARE} } }",
        mapOf(variable("name", name), variable("realPaths", realPaths), variable("readOnly", readOnly), variable("expiresAt", expiresAt)),
        field = "createShare",
    )

    fun updateShare(
        id: String,
        name: String,
        expiresAt: Instant?,
        realPaths: Collection<String>?,
    ): GraphqlRequest<ShareRecord> = GraphqlRequest(
        $$"mutation($id: ID!, $name: String!, $expiresAt: Instant, $realPaths: [String!]) { updateShare(id: $id, name: $name, expiresAt: $expiresAt, realPaths: $realPaths) { $${Fragments.SHARE} } }",
        mapOf(variable("id", id), variable("name", name), variable("expiresAt", expiresAt), variable("realPaths", realPaths)),
        field = "updateShare",
    )

    fun sharedDownloadControl(command: SharedDownloadCommand, id: String): GraphqlRequest<Unit> {
        val field = when (command) {
            SharedDownloadCommand.RETRY -> "retrySharedDownload"
            SharedDownloadCommand.CANCEL -> "cancelSharedDownload"
            SharedDownloadCommand.REMOVE -> "removeSharedDownload"
        }
        return GraphqlRequest(
            $$"mutation($id: ID!) { $${field}(id: $id) }",
            mapOf(variable("id", id)),
            field = field,
        )
    }

    fun enqueueChatDownload(messageId: String, fileId: String, peerId: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($messageId: ID!, $fileId: String!, $peerId: ID!) { enqueueChatDownload(messageId: $messageId, fileId: $fileId, peerId: $peerId) }",
        mapOf(variable("messageId", messageId), variable("fileId", fileId), variable("peerId", peerId)),
        field = "enqueueChatDownload",
    )

    fun chatDownloadControl(command: ChatDownloadCommand, id: String): GraphqlRequest<Unit> {
        val field = when (command) {
            ChatDownloadCommand.PAUSE -> "pauseChatDownload"
            ChatDownloadCommand.RESUME -> "resumeChatDownload"
            ChatDownloadCommand.RETRY -> "retryChatDownload"
            ChatDownloadCommand.REMOVE -> "removeChatDownload"
        }
        return GraphqlRequest(
            $$"mutation($id: ID!) { $${field}(id: $id) }",
            mapOf(variable("id", id)),
            field = field,
        )
    }

    fun setPref(key: String, value: kotlinx.serialization.json.JsonElement): GraphqlRequest<Boolean> = GraphqlRequest(
        $$"mutation($key: String!, $value: JSON!) { setPref(key: $key, value: $value) }",
        mapOf(variable("key", key), variable("value", value)),
        field = "setPref",
    )

    fun setSavedValue(key: String, value: JsonElement): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($key: String!, $value: JSON!) { setSavedValue(key: $key, value: $value) }",
        mapOf(variable("key", key), variable("value", value)),
        field = "setSavedValue",
    )

    fun syncFeedEntryContent(id: String): GraphqlRequest<FeedEntry> = GraphqlRequest(
        $$"mutation($id: ID!) { syncFeedEntryContent(id: $id) { $${Fragments.ENTRY_FIELDS} } }",
        mapOf(variable("id", id)),
        field = "syncFeedEntryContent",
    )

    fun mediaAction(
        action: MediaAction,
        type: DataType,
        query: String,
        destDir: String,
    ): GraphqlRequest<ActionResult> = when (action) {
        MediaAction.TRASH -> GraphqlRequest(
            $$"mutation($type: MediaDataType!, $query: String!) { trashMediaItems(type: $type, query: $query) { affectedCount } }",
            mapOf(variable("type", type.name), variable("query", query)),
            field = "trashMediaItems",
        )
        MediaAction.RESTORE -> GraphqlRequest(
            $$"mutation($type: MediaDataType!, $query: String!) { restoreMediaItems(type: $type, query: $query) { affectedCount } }",
            mapOf(variable("type", type.name), variable("query", query)),
            field = "restoreMediaItems",
        )
        MediaAction.DELETE -> GraphqlRequest(
            $$"mutation($type: MediaDataType!, $query: String!) { deleteMediaItems(type: $type, query: $query) { affectedCount } }",
            mapOf(variable("type", type.name), variable("query", query)),
            field = "deleteMediaItems",
        )
        MediaAction.MOVE -> GraphqlRequest(
            $$"mutation($type: MediaDataType!, $query: String!, $destDir: String!) { moveMediaItems(type: $type, query: $query, destDir: $destDir) { affectedCount } }",
            mapOf(variable("type", type.name), variable("query", query), variable("destDir", destDir)),
            field = "moveMediaItems",
        )
    }

    internal fun dlna(command: CastCommand): GraphqlRequest<CastSnapshot> = when (command) {
        CastCommand.StartScan -> GraphqlRequest(
            $$"mutation { startDlnaScan { $${Fragments.DLNA_STATE} } }",
            field = "startDlnaScan",
        )
        CastCommand.StopScan -> GraphqlRequest(
            $$"mutation { stopDlnaScan { $${Fragments.DLNA_STATE} } }",
            field = "stopDlnaScan",
        )
        is CastCommand.Select -> GraphqlRequest(
            $$"mutation($id: ID!) { selectDlnaDevice(id: $id) { $${Fragments.DLNA_STATE} } }",
            mapOf(variable("id", command.id)),
            field = "selectDlnaDevice",
        )
        is CastCommand.Cast -> GraphqlRequest(
            $$"mutation($item: DlnaMediaInput!) { castDlnaMedia(item: $item) { $${Fragments.DLNA_STATE} } }",
            mapOf(variable("item", command.item)),
            field = "castDlnaMedia",
        )
        CastCommand.Play -> GraphqlRequest(
            $$"mutation { playDlna { $${Fragments.DLNA_STATE} } }",
            field = "playDlna",
        )
        CastCommand.Pause -> GraphqlRequest(
            $$"mutation { pauseDlna { $${Fragments.DLNA_STATE} } }",
            field = "pauseDlna",
        )
        is CastCommand.Seek -> GraphqlRequest(
            $$"mutation($positionMs: Long!) { seekDlna(positionMs: $positionMs) { $${Fragments.DLNA_STATE} } }",
            mapOf(variable("positionMs", command.positionMs)),
            field = "seekDlna",
        )
        CastCommand.Stop -> GraphqlRequest(
            $$"mutation { stopDlna { $${Fragments.DLNA_STATE} } }",
            field = "stopDlna",
        )
        CastCommand.Exit -> GraphqlRequest(
            $$"mutation { exitDlna { $${Fragments.DLNA_STATE} } }",
            field = "exitDlna",
        )
        is CastCommand.Add -> GraphqlRequest(
            $$"mutation($item: DlnaMediaInput!) { addDlnaItem(item: $item) { $${Fragments.DLNA_STATE} } }",
            mapOf(variable("item", command.item)),
            field = "addDlnaItem",
        )
        is CastCommand.Remove -> GraphqlRequest(
            $$"mutation($path: String!) { removeDlnaItem(path: $path) { $${Fragments.DLNA_STATE} } }",
            mapOf(variable("path", command.path)),
            field = "removeDlnaItem",
        )
        is CastCommand.RemoveAt -> GraphqlRequest(
            $$"mutation($index: Int!) { removeDlnaItemAt(index: $index) { $${Fragments.DLNA_STATE} } }",
            mapOf(variable("index", command.index)),
            field = "removeDlnaItemAt",
        )
        is CastCommand.Reorder -> GraphqlRequest(
            $$"mutation($from: Int!, $to: Int!) { reorderDlnaItems(from: $from, to: $to) { $${Fragments.DLNA_STATE} } }",
            mapOf(variable("from", command.from), variable("to", command.to)),
            field = "reorderDlnaItems",
        )
        CastCommand.Clear -> GraphqlRequest(
            $$"mutation { clearDlnaItems { $${Fragments.DLNA_STATE} } }",
            field = "clearDlnaItems",
        )
    }

    fun saveSharedText(directory: String, path: String, content: String): GraphqlRequest<Unit> = GraphqlRequest(
        $$"mutation($directory: String!, $path: String!, $content: String!) { createDir(path: $directory) { path } writeTextFile(path: $path, content: $content, overwrite: true) { path } }",
        mapOf(variable("directory", directory), variable("path", path), variable("content", content)),
        field = "createDir",
    )
}
