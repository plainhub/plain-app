package com.ismartcoding.plain.graphql

import com.ismartcoding.plain.platform.system.DataType

internal object Fragments {
    const val CHAT_ITEM = "id fromId toId channelId content { ... on ChatTextContent { __typename text linkPreviews { url title description imageUrl imageLocalPath imageWidth imageHeight siteName domain createdAt } } ... on ChatImagesContent { __typename items { id uri size durationMs width height summary fileName } } ... on ChatFilesContent { __typename items { id uri size durationMs width height summary fileName } } ... on ChatShareContent { __typename shareId urlToken peerInfo { id ip port } name itemCount totalSize expiresAt } } createdAt updatedAt status deliveryResults { peerId peerName error }"
    const val CONTACT = "id prefix firstName middleName lastName suffix nickname photoId phoneNumbers { value type typeCode label normalizedNumber } emails { value type typeCode label } addresses { value type typeCode label } events { value type typeCode label } websites { value type typeCode label } ims { value protocol customProtocol typeCode } source starred contactId thumbnailId notes groups { id name } organization { company title } ringtone updatedAt"
    const val NEARBY_DEVICE_FIELDS = "id name signaturePublicKey ips port deviceType version platform lastSeen discoveryMethods bestIp status remoteAccess"
    const val MDNS_STATUS = "runtimeId revision running scanning"
    const val MDNS_DEBUG = "services { serviceType instanceName instanceFqdn hostname port txtRecords ips ip6s complete } packetsIn { time direction srcIp srcPort dstIp dstPort size summary detail } packetsOut { time direction srcIp srcPort dstIp dstPort size summary detail }"
    const val TAG_FIELDS = "id name type count"
    const val FEED_FIELDS = "id name url logo fetchContent lastSyncAt lastError { code detail } createdAt updatedAt"
    const val ENTRY_FIELDS = "id title url image description author content feedId rawId publishedAt read createdAt updatedAt"
    const val MEDIA_PLAY_HISTORY = "mediaId positionMs playedAt"
    const val AUDIO_TRACK_FIELDS = "mediaId path title artist durationMs"
    const val AUDIO_PLAYLIST_FIELDS = "id name itemCount createdAt updatedAt"
    const val NOTE_FIELDS = "id title content deletedAt createdAt updatedAt"
    const val BOOKMARK = "id url title faviconPath groupId pinned clickCount lastClickedAt sortOrder createdAt updatedAt"
    const val POMODORO = "date completedCount currentRound timeLeftSec totalTimeSec isRunning isPaused state"
    const val FILE = "name path url permission createdAt updatedAt size isDir childCount mediaId isSystem"
    const val FAVORITE_FOLDER = "rootPath fullPath alias"
    const val FILE_TASK = "id clientId type status title error totalBytes doneBytes totalItems doneItems createdAt updatedAt completedOps { src dst }"
    const val SHARE = "id name password readOnly expiresAt data { virtualPath realPath isDir } url"
    const val MEDIA_COMMON = "id title path size bucketId createdAt updatedAt"
    const val CLIPBOARD_ITEM = "id text source label sensitive createdAt"
    const val APP_FILE = "id size mimeType realPath createdAt updatedAt fileName"
    const val SMS = "id body address sentAt serviceCenter read threadId type typeCode subscriptionId isMms attachments { path contentType name }"
    const val DLNA_STATE = "version devices { id hostAddress name } currentDevice { id hostAddress name } items { path title albumArt artist durationMs type } currentUri playing progressMs durationMs supportsCallback"
    const val IMAGE_EDITOR_SUMMARY = "id thumbnail canvasWidth canvasHeight layerCount updatedAt"
    const val IMAGE_EDITOR_PROJECT = $$"$$IMAGE_EDITOR_SUMMARY stateB64 createdAt"
    const val PEER_SUMMARY = "id name ip publicKey status port deviceType createdAt updatedAt online managed"
    const val PEER = "id name ip key publicKey status port deviceType token createdAt updatedAt online managed"
    const val CHANNEL_FIELDS = "id ownerId name actions { isOwner canRename canManageMembers canLeave canDelete } members { peerId status } version status createdAt updatedAt"
    const val DOWNLOAD = "id title targetDir zipName status downloadedBytes totalBytes bytesPerSecond totalFiles doneFiles currentFile error failures { path error } source { __typename ... on ResourceDownloadSource { urls } ... on ChatDownloadSource { messageId peerId fileIds } }"

    fun mediaFields(type: DataType): String {
        val extra = when (type) {
            DataType.AUDIO -> "artist durationMs albumId albumFileId isFavorite"
            DataType.IMAGE -> "width height rotation takenAt isFavorite"
            DataType.VIDEO -> "durationMs width height rotation takenAt isFavorite"
            DataType.DOC -> "extension"
            else -> error("Unsupported media type: $type")
        }
        return $$"$$MEDIA_COMMON $$extra"
    }
}
