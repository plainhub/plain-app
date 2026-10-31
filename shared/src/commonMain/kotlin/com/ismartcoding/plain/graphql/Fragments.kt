package com.ismartcoding.plain.graphql

import com.ismartcoding.plain.platform.system.DataType

internal object Fragments {
    const val CHAT_ITEM = "id fromId toId channelId content status statusData createdAt updatedAt"
    const val CONTACT = "id prefix firstName middleName lastName suffix nickname photoId phoneNumbers { value type typeCode label normalizedNumber } emails { value type typeCode label } addresses { value type typeCode label } events { value type typeCode label } websites { value type typeCode label } ims { value protocol customProtocol typeCode } source starred contactId thumbnailId notes groups { id name } organization { company title } ringtone updatedAt"
    const val PAIRING_STATE = "deviceId generation mode phase error verificationCode"
    const val NEARBY_DEVICE_FIELDS = "id name ips port deviceType version platform lastSeen discoveryMethods bestIp status"
    const val MDNS_STATUS = "runtimeId revision running scanning"
    const val MDNS_DEBUG = "services { serviceType instanceName instanceFqdn hostname port txtRecords ips ipv6 complete } packetsIn { time direction srcIp srcPort dstIp dstPort size summary detail } packetsOut { time direction srcIp srcPort dstIp dstPort size summary detail }"
    const val TAG_FIELDS = "id name type count"
    const val FEED_FIELDS = "id name url logo fetchContent lastSyncAt lastError { code detail } createdAt updatedAt"
    const val ENTRY_FIELDS = "id title url image description author content feedId rawId publishedAt read createdAt updatedAt"
    const val MEDIA_PLAY_HISTORY = "mediaId positionMs playedAt"
    const val AUDIO_TRACK_FIELDS = "mediaId path title artist durationMs"
    const val AUDIO_PLAYLIST_FIELDS = "id name itemCount createdAt updatedAt"
    const val NOTE_FIELDS = "id title content deletedAt createdAt updatedAt"
    const val BOOKMARK = "id url title faviconPath groupId pinned clickCount lastClickedAt sortOrder createdAt updatedAt"
    const val POMODORO = "date completedCount currentRound timeLeftSec totalTimeSec isRunning isPaused state"
    const val FILE = "name path permission createdAt updatedAt size isDir childCount mediaId"
    const val FAVORITE_FOLDER = "rootPath fullPath alias"
    const val FILE_TASK = "id clientId type status title error totalBytes doneBytes totalItems doneItems createdAt updatedAt completedOps { src dst }"
    const val SHARE_LINK = "host port sharedId token pageUrl"
    const val SHARED_ENTRY = "name relativePath isDir size mimeType hasThumb"
    const val SHARED_INFO = $$"name readOnly requiresPassword expiresAt urlToken entries { $${SHARED_ENTRY} }"
    const val SHARED_CARD = "shareId urlToken peerInfo { id ip port } name itemCount totalSize expiresAt"
    const val SHARE = "id name password urlToken readOnly expiresAt createdAt updatedAt roots { relativePath realPath isDir }"
    const val SHARED_DOWNLOAD_QUEUE = "runtimeId revision tasks { id messageId type title targetDir link { host port sharedId token pageUrl } urlToken entries { name relativePath isDir size mimeType hasThumb } zipName generation status error downloadedSize totalSize downloadSpeed totalFiles doneFiles failedFiles currentFile packing failures { path error } }"
    const val MEDIA_COMMON = "id title path size bucketId createdAt updatedAt"
    const val CLIPBOARD_ITEM = "id text source label sensitive createdAt"
    const val APP_FILE = "id size mimeType realPath createdAt updatedAt fileName"
    const val SMS = "id body address sentAt serviceCenter read threadId type typeCode subscriptionId isMms attachments { path contentType name }"
    const val DLNA_STATE = "version devices { id hostAddress name } currentDevice { id hostAddress name } items { path title albumArt artist durationMs audio } currentUri playing progressMs durationMs supportsCallback"
    const val IMAGE_EDITOR_SUMMARY = "id thumbnail canvasWidth canvasHeight layerCount updatedAt"
    const val IMAGE_EDITOR_PROJECT = $$"$$IMAGE_EDITOR_SUMMARY stateB64 createdAt"
    const val PEER_SUMMARY = "id name ip publicKey status port deviceType createdAt updatedAt online"
    const val PEER = "id name ip key publicKey status port deviceType token createdAt updatedAt online address { bestIp name baseUrl apiUrl statusWsUrl }"
    const val CHANNEL_FIELDS = "id ownerId name actions { isOwner canRename canManageMembers canLeave canDelete } members { peerId status } version status createdAt updatedAt"
    private const val CHAT_DOWNLOAD_TASKS = "runtimeId revision tasks { id messageId status error downloaded total speed file { id: fileId uri size durationMs width height summary fileName } "
    const val CHAT_DOWNLOAD_QUEUE = CHAT_DOWNLOAD_TASKS + "peer { " + PEER + " } }"
    const val CHAT_DOWNLOAD_QUEUE_SUMMARY = CHAT_DOWNLOAD_TASKS + "peer { " + PEER_SUMMARY + " } }"

    fun mediaFields(type: DataType): String {
        val extra = when (type) {
            DataType.AUDIO -> "artist durationMs albumId albumFileId isFavorite"
            DataType.IMAGE -> "width height rotation takenAt isFavorite"
            DataType.VIDEO -> "durationMs width height rotation takenAt isFavorite"
            DataType.DOC -> "durationMs extension"
            else -> error("Unsupported media type: $type")
        }
        return $$"$$MEDIA_COMMON $$extra"
    }
}
