package com.ismartcoding.plain.graphql

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.models.AppFileName
import com.ismartcoding.plain.models.AudioDuration
import com.ismartcoding.plain.models.ChatDownloadSnapshot
import com.ismartcoding.plain.models.DocExtensionGroup
import com.ismartcoding.plain.models.FeedsResponse
import com.ismartcoding.plain.models.FileSize
import com.ismartcoding.plain.models.MediaFileInfoResult
import com.ismartcoding.plain.models.MergeResult
import com.ismartcoding.plain.models.ProjectSummary
import com.ismartcoding.plain.models.QueuedMediaId
import com.ismartcoding.plain.models.SelectedMediaPath
import com.ismartcoding.plain.models.SharedDownloadSnapshot
import com.ismartcoding.plain.features.chat.models.MessageShare
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.nearby.MdnsSnapshot
import com.ismartcoding.plain.features.nearby.PairingState
import com.ismartcoding.plain.features.sessions.SessionFacts
import com.ismartcoding.plain.features.shares.SharedLink
import com.ismartcoding.plain.models.AppFile
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.models.AudioOverview
import com.ismartcoding.plain.models.AudioPlayback
import com.ismartcoding.plain.models.AudioPlaylist
import com.ismartcoding.plain.models.AudioPlaylistCovers
import com.ismartcoding.plain.models.Call
import com.ismartcoding.plain.models.ChatChannel
import com.ismartcoding.plain.models.ChatConversation
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.models.ClipboardItem
import com.ismartcoding.plain.models.Contact
import com.ismartcoding.plain.models.Doc
import com.ismartcoding.plain.models.FavoriteFolder
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.models.FeedPreview
import com.ismartcoding.plain.models.FeedSyncState
import com.ismartcoding.plain.models.File as FileEntry
import com.ismartcoding.plain.models.Image
import com.ismartcoding.plain.models.ImageEditorProject
import com.ismartcoding.plain.models.ImageFileInfo
import com.ismartcoding.plain.models.MediaBucket
import com.ismartcoding.plain.models.MediaPlayHistory
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.models.Note
import com.ismartcoding.plain.models.Package as AppPackage
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PeerRecordInput
import com.ismartcoding.plain.models.PomodoroToday
import com.ismartcoding.plain.models.ShareRecord
import com.ismartcoding.plain.models.Sms
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.models.Video
import com.ismartcoding.plain.models.VideoFileInfo
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.variable

object Query {
    fun appPermissions(): GraphqlRequest<List<com.ismartcoding.plain.platform.permissions.Permission>> =
        GraphqlRequest<com.ismartcoding.plain.models.AppPermissions>("query { app { permissions } }", field = "app").map { it.permissions }

    fun chatConversations(): GraphqlRequest<List<ChatConversation>> = GraphqlRequest(
        $$"query { chatConversations { id chat { $${Fragments.CHAT_ITEM} } } }",
        field = "chatConversations",
    )

    internal fun peerSummaries(): GraphqlRequest<List<Peer>> = GraphqlRequest(
        "query { peers { ${Fragments.PEER_SUMMARY} } }", field = "peers",
    )

    fun chatCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { chatCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "chatCount",
    )

    fun chatRecords(query: String, offset: Int, limit: Int): GraphqlRequest<List<ChatItem>> = GraphqlRequest(
        $$"query($query: String!, $offset: Int!, $limit: Int!) { chatRecords(query: $query, offset: $offset, limit: $limit) { $${Fragments.CHAT_ITEM} } }",
        mapOf(variable("query", query), variable("offset", offset), variable("limit", limit)),
        field = "chatRecords",
    )

    fun chatItem(id: String): GraphqlRequest<ChatItem?> = GraphqlRequest(
        $$"query($id: ID!) { chatItem(id: $id) { $${Fragments.CHAT_ITEM} } }",
        mapOf(variable("id", id)),
        field = "chatItem",
    )

    fun contacts(query: String, limit: Int, offset: Int): GraphqlRequest<List<Contact>> = GraphqlRequest(
        $$"query($query: String!, $limit: Int!, $offset: Int!) { contacts(query: $query, limit: $limit, offset: $offset) { $${Fragments.CONTACT} } }",
        mapOf(variable("query", query), variable("limit", limit), variable("offset", offset)),
        field = "contacts",
    )

    fun contactCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { contactCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "contactCount",
    )

    fun pairingStates(): GraphqlRequest<List<PairingState>> = GraphqlRequest(
        $$"query { pairingStates { $${Fragments.PAIRING_STATE} } }",
        field = "pairingStates",
    )

    fun nearbyDevices(): GraphqlRequest<List<NearbyDevice>> = GraphqlRequest(
        $$"query { nearbyDevices { $${Fragments.NEARBY_DEVICE_FIELDS} } }",
        field = "nearbyDevices",
    )

    fun parsePairingCode(code: String): GraphqlRequest<NearbyDevice?> = GraphqlRequest(
        $$"query($code: String!) { parsePairingCode(code: $code) { $${Fragments.NEARBY_DEVICE_FIELDS} } }",
        mapOf(variable("code", code)),
        field = "parsePairingCode",
    )

    fun mdnsState(debug: Boolean = false): GraphqlRequest<MdnsSnapshot> = GraphqlRequest(
        "query { mdnsState { ${Fragments.MDNS_STATUS} ${if (debug) Fragments.MDNS_DEBUG else ""} } }",
        field = "mdnsState",
    )

    fun tags(type: DataType): GraphqlRequest<List<Tag>> = GraphqlRequest(
        $$"query($type: DataType!) { tags(type: $type) { $${Fragments.TAG_FIELDS} } }",
        mapOf(variable("type", type.name)),
        field = "tags",
    )

    fun tagKeys(id: String): GraphqlRequest<List<String>> = GraphqlRequest(
        $$"query($id: ID!) { tagKeys(id: $id) }",
        mapOf(variable("id", id)),
        field = "tagKeys",
    )

    fun tagRelations(type: DataType, keys: Collection<String>): GraphqlRequest<List<TagRelation>> = GraphqlRequest(
        $$"query($type: DataType!, $keys: [String!]!) { tagRelations(type: $type, keys: $keys) { tagId key } }",
        mapOf(variable("type", type.name), variable("keys", keys)),
        field = "tagRelations",
    )

    fun feedEntryCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { feedEntryCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "feedEntryCount",
    )

    fun feed(id: String): GraphqlRequest<Feed?> = GraphqlRequest(
        $$"query($id: ID!) { feed(id: $id) { $${Fragments.FEED_FIELDS} } }",
        mapOf(variable("id", id)),
        field = "feed",
    )

    fun feedPreview(url: String): GraphqlRequest<FeedPreview> = GraphqlRequest(
        $$"query($url: String!) { feedPreview(url: $url) { title } }",
        mapOf(variable("url", url)),
        field = "feedPreview",
    )

    fun feedSyncStates(): GraphqlRequest<List<FeedSyncState>> = GraphqlRequest(
        $$"query { feedSyncStates { feedId taskId revision status error } }",
        field = "feedSyncStates",
    )

    fun calls(query: String, limit: Int, offset: Int): GraphqlRequest<List<Call>> = GraphqlRequest(
        $$"query($query: String!, $limit: Int!, $offset: Int!) { calls(query: $query, limit: $limit, offset: $offset) { id number name photoId photoUri startedAt durationSec type typeCode accountId } }",
        mapOf(variable("query", query), variable("limit", limit), variable("offset", offset)),
        field = "calls",
    )

    fun callCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { callCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "callCount",
    )

    fun mediaPlayHistory(query: String, offset: Int, limit: Int): GraphqlRequest<List<MediaPlayHistory>> = GraphqlRequest(
        $$"query($query: String!, $offset: Int!, $limit: Int!) { mediaPlayHistory(query: $query, offset: $offset, limit: $limit) { $${Fragments.MEDIA_PLAY_HISTORY} } }",
        mapOf(variable("query", query), variable("offset", offset), variable("limit", limit)),
        field = "mediaPlayHistory",
    )

    fun audioPlaylistCovers(ids: Collection<String>): GraphqlRequest<List<AudioPlaylistCovers>> = GraphqlRequest(
        $$"query($ids: [ID!]!) { audioPlaylistCovers(ids: $ids) { id albums { albumId trackCount candidatePaths } } }",
        mapOf(variable("ids", ids)),
        field = "audioPlaylistCovers",
    )

    fun audioPlaylists(): GraphqlRequest<List<AudioPlaylist>> = GraphqlRequest(
        $$"query { audioPlaylists { $${Fragments.AUDIO_PLAYLIST_FIELDS} } }",
        field = "audioPlaylists",
    )

    fun audioPlaylistItems(id: String, offset: Int, limit: Int, query: String): GraphqlRequest<List<AudioItem>> = GraphqlRequest(
        $$"query($id: ID!, $offset: Int!, $limit: Int!, $query: String!) { audioPlaylistItems(id: $id, offset: $offset, limit: $limit, query: $query) { $${Fragments.AUDIO_TRACK_FIELDS} } }",
        mapOf(variable("id", id), variable("offset", offset), variable("limit", limit), variable("query", query)),
        field = "audioPlaylistItems",
    )

    internal fun audioPlaybackSettings(): GraphqlRequest<com.ismartcoding.plain.models.AudioPlaybackSettings> = GraphqlRequest(
        "query { audioPlayback { mediaId path mode isPlaying positionMs revision } prefs }",
    )

    internal fun audioQueueSnapshot(limit: Int): GraphqlRequest<com.ismartcoding.plain.models.AudioQueueSnapshot> = GraphqlRequest(
        $$"query($limit: Int!) { audioPlayback { mediaId path mode isPlaying positionMs revision } prefs count: audioQueueItemCount items: audioQueueItems(query: \"\", offset: 0, limit: $limit) { $${Fragments.AUDIO_TRACK_FIELDS} } queuedIds: audioQueueItems(query: \"\", offset: 0, limit: 2147483647) { mediaId } }",
        mapOf(variable("limit", limit)),
    )

    fun audioPlayback(): GraphqlRequest<AudioPlayback> = GraphqlRequest(
        $$"query { audioPlayback { mediaId path mode isPlaying positionMs revision } }",
        field = "audioPlayback",
    )

    fun audioQueueIds(limit: Int): GraphqlRequest<List<QueuedMediaId>> = GraphqlRequest(
        $$"query($limit: Int!) { audioQueueItems(query: \"\", offset: 0, limit: $limit) { mediaId } }",
        mapOf(variable("limit", limit)),
        field = "audioQueueItems",
    )

    fun audioQueueItemCount(): GraphqlRequest<Int> = GraphqlRequest(
        $$"query { audioQueueItemCount }",
        field = "audioQueueItemCount",
    )

    fun audioQueueItems(offset: Int, limit: Int, query: String): GraphqlRequest<List<AudioItem>> = GraphqlRequest(
        $$"query($offset: Int!, $limit: Int!, $query: String!) { audioQueueItems(offset: $offset, limit: $limit, query: $query) { $${Fragments.AUDIO_TRACK_FIELDS} } }",
        mapOf(variable("offset", offset), variable("limit", limit), variable("query", query)),
        field = "audioQueueItems",
    )

    internal fun dlnaSenderState(): GraphqlRequest<com.ismartcoding.plain.features.dlna.sender.CastSnapshot> = GraphqlRequest(
        "query { dlnaSenderState { ${Fragments.DLNA_STATE} } }",
        field = "dlnaSenderState",
    )

    internal fun sessions(): GraphqlRequest<List<SessionFacts>> = GraphqlRequest(
        $$"query { sessions { clientId clientName type token clientIp osName osVersion browserName browserVersion createdAt updatedAt lastSeenAt } }",
        field = "sessions",
    )

    fun noteCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { noteCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "noteCount",
    )

    fun notes(query: String, limit: Int, offset: Int): GraphqlRequest<List<Note>> = GraphqlRequest(
        $$"query($query: String!, $limit: Int!, $offset: Int!) { notes(query: $query, limit: $limit, offset: $offset) { $${Fragments.NOTE_FIELDS} } }",
        mapOf(variable("query", query), variable("limit", limit), variable("offset", offset)),
        field = "notes",
    )

    fun note(id: String): GraphqlRequest<Note?> = GraphqlRequest(
        $$"query($id: ID!) { note(id: $id) { $${Fragments.NOTE_FIELDS} } }",
        mapOf(variable("id", id)),
        field = "note",
    )

    fun packages(query: String, offset: Int, limit: Int, sortBy: FileSortBy): GraphqlRequest<List<AppPackage>> = GraphqlRequest(
        $$"query($query: String!, $offset: Int!, $limit: Int!, $sortBy: FileSortBy!) { packages(query: $query, offset: $offset, limit: $limit, sortBy: $sortBy) { id name type version path size installedAt updatedAt certs { issuer subject serialNumber validFrom validTo } } }",
        mapOf(variable("query", query), variable("offset", offset), variable("limit", limit), variable("sortBy", sortBy.name)),
        field = "packages",
    )

    fun packageManifest(id: String): GraphqlRequest<String> = GraphqlRequest(
        $$"query($id: ID!) { packageManifest(id: $id) }",
        mapOf(variable("id", id)),
        field = "packageManifest",
    )

    fun packageCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { packageCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "packageCount",
    )

    fun pomodoroToday(): GraphqlRequest<PomodoroToday> = GraphqlRequest(
        $$"query { pomodoroToday { $${Fragments.POMODORO} } }",
        field = "pomodoroToday",
    )

    fun fileInfo(path: String): GraphqlRequest<FileSize> = GraphqlRequest(
        $$"query($path: String!) { fileInfo(path: $path) { size } }",
        mapOf(variable("path", path)),
        field = "fileInfo",
    )

    fun indexedFiles(query: String, limit: Int, offset: Int, sortBy: FileSortBy): GraphqlRequest<List<FileEntry>> = GraphqlRequest(
        $$"query($query: String!, $limit: Int!, $offset: Int!, $sortBy: FileSortBy!) { indexedFiles(query: $query, limit: $limit, offset: $offset, sortBy: $sortBy) { $${Fragments.FILE} } }",
        mapOf(variable("query", query), variable("limit", limit), variable("offset", offset), variable("sortBy", sortBy.name)),
        field = "indexedFiles",
    )

    fun recentFiles(): GraphqlRequest<List<FileEntry>> = GraphqlRequest(
        $$"query { recentFiles { $${Fragments.FILE} } }",
        field = "recentFiles",
    )

    fun indexedFileCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { indexedFileCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "indexedFileCount",
    )

    fun files(
        root: String,
        query: String,
        sortBy: FileSortBy,
        offset: Int,
        limit: Int,
    ): GraphqlRequest<List<FileEntry>> = GraphqlRequest(
        $$"query($root: String!, $query: String!, $sortBy: FileSortBy!, $offset: Int!, $limit: Int!) { files(root: $root, query: $query, sortBy: $sortBy, offset: $offset, limit: $limit) { $${Fragments.FILE} } }",
        mapOf(variable("root", root), variable("query", query), variable("sortBy", sortBy.name), variable("offset", offset), variable("limit", limit)),
        field = "files",
    )

    fun favoriteFolders(): GraphqlRequest<List<FavoriteFolder>> = GraphqlRequest(
        $$"query { favoriteFolders { $${Fragments.FAVORITE_FOLDER} } }",
        field = "favoriteFolders",
    )

    fun initialSharedLink(messageId: String, expected: MessageShare): GraphqlRequest<SharedLink> = GraphqlRequest(
        $$"query($messageId: ID!, $expected: SharedMessageCardInput!) { initialSharedLink(messageId: $messageId, expected: $expected) { $${Fragments.SHARE_LINK} } }",
        mapOf(variable("messageId", messageId), variable("expected", expected)),
        field = "initialSharedLink",
    )

    fun shareRecords(): GraphqlRequest<List<ShareRecord>> = GraphqlRequest(
        $$"query { shareRecords { $${Fragments.SHARE} } }",
        field = "shareRecords",
    )

    fun shareRecord(id: String): GraphqlRequest<ShareRecord?> = GraphqlRequest(
        $$"query($id: ID!) { shareRecord(id: $id) { $${Fragments.SHARE} } }",
        mapOf(variable("id", id)),
        field = "shareRecord",
    )

    fun sharedDownloadQueue(): GraphqlRequest<SharedDownloadSnapshot> = GraphqlRequest(
        $$"query { sharedDownloadQueue { $${Fragments.SHARED_DOWNLOAD_QUEUE} } }",
        field = "sharedDownloadQueue",
    )

    fun docExtGroups(query: String): GraphqlRequest<List<DocExtensionGroup>> = GraphqlRequest(
        $$"query($query: String!) { docExtGroups(query: $query) { ext count } }",
        mapOf(variable("query", query)),
        field = "docExtGroups",
    )

    fun mediaBuckets(type: DataType): GraphqlRequest<List<MediaBucket>> = GraphqlRequest(
        $$"query($type: MediaDataType!) { mediaBuckets(type: $type) { id name itemCount size topItemPaths } }",
        mapOf(variable("type", type.name)),
        field = "mediaBuckets",
    )

    fun audioMetadata(path: String): GraphqlRequest<AudioItem> = GraphqlRequest(
        $$"query AudioMetadata($path: String!) { audioMetadata(path: $path) { title artist path durationMs } }",
        mapOf(variable("path", path)),
        field = "audioMetadata",
    )

    fun audioLyricsByPath(path: String): GraphqlRequest<String?> = GraphqlRequest(
        $$"query($path: String!) { audioLyricsByPath(path: $path) }",
        mapOf(variable("path", path)),
        field = "audioLyricsByPath",
    )

    fun videoFileInfo(path: String): GraphqlRequest<MediaFileInfoResult<VideoFileInfo>> = GraphqlRequest(
        $$"query($path:String!,$name:String!){fileInfo(path:$path,fileName:$name){data{__typename ... on VideoFileInfo{width height rotation displayWidth displayHeight durationMs bitrate frameRate title artist album genre takenAt writer composer location{latitude longitude}}}}}",
        mapOf(variable("path", path), variable("name", "video.mp4")),
        field = "fileInfo",
    )

    fun imageFileInfo(path: String): GraphqlRequest<MediaFileInfoResult<ImageFileInfo>> = GraphqlRequest(
        $$"query($path:String!,$name:String!){fileInfo(path:$path,fileName:$name){data{__typename ... on ImageFileInfo{width height rotation make model colorSpace apertureValue exposureTime focalLength isoSpeed takenAt flash fNumber exposureProgram meteringMode whiteBalance creator resolutionX resolutionY description location{latitude longitude}}}}}",
        mapOf(variable("path", path), variable("name", "image.jpg")),
        field = "fileInfo",
    )

    fun audioDuration(path: String): GraphqlRequest<MediaFileInfoResult<AudioDuration>> = GraphqlRequest(
        $$"query($path:String!,$name:String!){fileInfo(path:$path,fileName:$name){data{... on AudioFileInfo{durationMs}}}}",
        mapOf(variable("path", path), variable("name", "audio.mp3")),
        field = "fileInfo",
    )

    fun clipboardItems(limit: Int, offset: Int, query: String): GraphqlRequest<List<ClipboardItem>> = GraphqlRequest(
        $$"query($limit: Int!, $offset: Int!, $query: String!) { clipboardItems(limit: $limit, offset: $offset, query: $query) { $${Fragments.CLIPBOARD_ITEM} } }",
        mapOf(variable("limit", limit), variable("offset", offset), variable("query", query)),
        field = "clipboardItems",
    )

    fun clipboardItemCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { clipboardItemCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "clipboardItemCount",
    )

    fun mergeStatus(fileId: String): GraphqlRequest<MergeResult> = GraphqlRequest(
        $$"query($fileId: String!) { mergeStatus(fileId: $fileId) { status value error } }",
        mapOf(variable("fileId", fileId)),
        field = "mergeStatus",
    )

    fun appFiles(offset: Int, limit: Int, query: String): GraphqlRequest<List<AppFile>> = GraphqlRequest(
        $$"query($offset: Int!, $limit: Int!, $query: String!) { appFiles(offset: $offset, limit: $limit, query: $query) { $${Fragments.APP_FILE} } }",
        mapOf(variable("offset", offset), variable("limit", limit), variable("query", query)),
        field = "appFiles",
    )

    fun appFileCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { appFileCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "appFileCount",
    )

    fun appFileName(query: String): GraphqlRequest<List<AppFileName>> = GraphqlRequest(
        $$"query($query: String!) { appFiles(offset: 0, limit: 1, query: $query) { fileName } }",
        mapOf(variable("query", query)),
        field = "appFiles",
    )

    fun sms(query: String, offset: Int, limit: Int): GraphqlRequest<List<Sms>> = GraphqlRequest(
        $$"query($query: String!, $offset: Int!, $limit: Int!) { sms(query: $query, offset: $offset, limit: $limit) { $${Fragments.SMS} } }",
        mapOf(variable("query", query), variable("offset", offset), variable("limit", limit)),
        field = "sms",
    )

    fun smsCount(query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { smsCount(query: $query) }",
        mapOf(variable("query", query)),
        field = "smsCount",
    )

    fun imageEditorProjects(): GraphqlRequest<List<ProjectSummary>> = GraphqlRequest(
        $$"query { imageEditorProjects { $${Fragments.IMAGE_EDITOR_SUMMARY} } }",
        field = "imageEditorProjects",
    )

    fun imageEditorProject(id: String): GraphqlRequest<ImageEditorProject?> = GraphqlRequest(
        $$"query($id: ID!) { imageEditorProject(id: $id) { $${Fragments.IMAGE_EDITOR_PROJECT} } }",
        mapOf(variable("id", id)),
        field = "imageEditorProject",
    )

    fun peers(): GraphqlRequest<List<Peer>> = GraphqlRequest(
        $$"query { peers { $${Fragments.PEER} } }",
        field = "peers",
    )

    internal fun peerFileUrl(id: String, expected: PeerRecordInput, fileId: String): GraphqlRequest<String> = GraphqlRequest(
        $$"query($id: ID!, $expected: PeerRecordInput!, $fileId: String!) { peerFileUrl(id: $id, expected: $expected, fileId: $fileId) }",
        mapOf(variable("id", id), variable("expected", expected), variable("fileId", fileId)),
        field = "peerFileUrl",
    )

    fun chatChannels(): GraphqlRequest<List<ChatChannel>> = GraphqlRequest(
        $$"query { chatChannels { $${Fragments.CHANNEL_FIELDS} } }",
        field = "chatChannels",
    )

    fun chatDownloadQueue(includePeerCredentials: Boolean = true): GraphqlRequest<ChatDownloadSnapshot> = GraphqlRequest(
        "query { chatDownloadQueue { ${if (includePeerCredentials) Fragments.CHAT_DOWNLOAD_QUEUE else Fragments.CHAT_DOWNLOAD_QUEUE_SUMMARY} } }",
        field = "chatDownloadQueue",
    )

    fun appLogFilesSize(): GraphqlRequest<Long> = GraphqlRequest(
        $$"query { appLogFilesSize }",
        field = "appLogFilesSize",
    )

    fun appCacheSize(): GraphqlRequest<Long> = GraphqlRequest(
        $$"query { appCacheSize }",
        field = "appCacheSize",
    )

    fun feeds(withCount: Boolean): GraphqlRequest<FeedsResponse> {
        val document = $$"query { feeds { $${Fragments.FEED_FIELDS} }" + (if (withCount) " feedEntryCounts { id count }" else "") + " }"
        return if (withCount) GraphqlRequest(document)
        else GraphqlRequest<List<Feed>>(document, field = "feeds").map { FeedsResponse(it, emptyList()) }
    }

    fun audioOverview(): GraphqlRequest<AudioOverview> = GraphqlRequest(
        $$"query { audioOverview { artists { name itemCount playCount samplePath } recentItems { $${Fragments.mediaFields(DataType.AUDIO)} } } }",
        field = "audioOverview",
    )

    fun shareLink(id: String, host: String?): GraphqlRequest<SharedLink> = GraphqlRequest(
        $$"query($id: ID!, $host: String) { shareLink(id: $id, host: $host) { $${Fragments.SHARE_LINK} } }",
        mapOf(variable("id", id), variable("host", host)),
        field = "shareLink",
    )

    fun mediaRoot(type: DataType): String = when (type) {
        DataType.AUDIO -> "audios"
        DataType.IMAGE -> "images"
        DataType.VIDEO -> "videos"
        DataType.DOC -> "docs"
        else -> error("Unsupported media type: $type")
    }

    fun mediaRows(
        type: DataType,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): GraphqlRequest<List<IData>> {
        val document = $$"query($query: String!, $limit: Int!, $offset: Int!, $sortBy: FileSortBy!) { $${mediaRoot(type)}(query: $query, limit: $limit, offset: $offset, sortBy: $sortBy) { $${Fragments.mediaFields(type)} } }"
        val variables = mapOf(variable("query", query), variable("limit", limit), variable("offset", offset), variable("sortBy", sortBy.name))
        val field = mediaRoot(type)
        return when (type) {
            DataType.AUDIO -> GraphqlRequest<List<Audio>>(document, variables, field = field)
            DataType.IMAGE -> GraphqlRequest<List<Image>>(document, variables, field = field)
            DataType.VIDEO -> GraphqlRequest<List<Video>>(document, variables, field = field)
            DataType.DOC -> GraphqlRequest<List<Doc>>(document, variables, field = field)
            else -> error("Unsupported media type: $type")
        }
    }

    fun mediaCountField(type: DataType): String {
        mediaRoot(type)
        return type.name.lowercase() + "Count"
    }

    fun mediaCount(type: DataType, query: String): GraphqlRequest<Int> = GraphqlRequest(
        $$"query($query: String!) { $${mediaCountField(type)}(query: $query) }",
        mapOf(variable("query", query)),
        field = mediaCountField(type),
    )

    fun selectedMediaPaths(root: String, query: String, limit: Int): GraphqlRequest<List<SelectedMediaPath>> {
        require(root in setOf("audios", "images", "videos", "docs")) { "Unsupported media root" }
        return GraphqlRequest(
            $$"query($query: String!, $limit: Int!) { $${root}(query: $query, limit: $limit, offset: 0, sortBy: NAME_ASC) { id path } }",
            mapOf(variable("query", query), variable("limit", limit)),
            field = root,
        )
    }

    fun chatSnapshot(ids: List<String>): GraphqlRequest<Map<String, ChatItem?>> {
        require(ids.isNotEmpty()) { "Chat snapshot requires IDs" }
        val definitions = ids.indices.joinToString(", ") { index -> $$"$id$${index}: ID!" }
        val selections = ids.indices.joinToString(" ") { index -> $$"message$${index}:chatItem(id: $id$${index}) { $${Fragments.CHAT_ITEM} }" }
        val variables = ids.mapIndexed { index, id -> variable("id$index", id) }.toMap()
        return GraphqlRequest($$"query($${definitions}) { $${selections} }", variables)
    }

    fun prefs(): GraphqlRequest<Map<String, kotlinx.serialization.json.JsonElement>> =
        GraphqlRequest("query { prefs }", field = "prefs")

    inline fun <reified T> savedValue(key: String, default: T): GraphqlRequest<T> = GraphqlRequest<T?>(
        $$"query($key: String!) { savedValue(key: $key) }",
        mapOf(variable("key", key)),
        field = "savedValue",
    ).map { it ?: default }

    fun feedEntries(query: String, limit: Int, offset: Int): GraphqlRequest<List<FeedEntry>> = GraphqlRequest(
        $$"query($query: String!, $limit: Int!, $offset: Int!) { feedEntries(query: $query, limit: $limit, offset: $offset) { $${Fragments.ENTRY_FIELDS} } }",
        mapOf(variable("query", query), variable("limit", limit), variable("offset", offset)),
        field = "feedEntries",
    )

    fun feedEntry(id: String): GraphqlRequest<FeedEntry?> = GraphqlRequest(
        $$"query($id: ID!) { feedEntry(id: $id) { $${Fragments.ENTRY_FIELDS} } }",
        mapOf(variable("id", id)),
        field = "feedEntry",
    )
}
