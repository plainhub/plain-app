package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.chat.channel.channelFacts
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object SystemProviderHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemPackageFacts",
        "systemPackageStatuses",
        "systemInstallPackage",
        "systemUninstallPackages" -> SystemPackagesHost.handle(method, params)
        "systemNotificationFacts",
        "systemCancelNotifications",
        "systemReplyNotification" -> SystemNotificationsHost.handle(method, params)
        "systemContactFacts",
        "systemContactCount",
        "systemContactIds",
        "systemContactGroupFacts",
        "systemContactSources",
        "systemCreateContact",
        "systemUpdateContact",
        "systemCreateContactGroup",
        "systemUpdateContactGroup",
        "systemDeleteContactGroup" -> SystemContactsHost.handle(method, params)
        "systemCallFacts",
        "systemCallCount",
        "systemCallIds",
        "systemMakeCall" -> SystemCallsHost.handle(method, params)
        "systemMmsTextFacts",
        "systemSmsCountFacts",
        "systemSmsRowsFacts",
        "systemSmsIdsFacts",
        "systemSmsConversationFacts",
        "systemSmsThreadFacts",
        "systemSendSms",
        "systemTrashSms",
        "systemRestoreSms",
        "systemDeleteSms",
        "systemSendMms",
        "systemSimFacts" -> SystemSmsHost.handle(method, params)
        "systemScreenMirrorState",
        "systemScreenMirrorQuality",
        "systemStartScreenMirror",
        "systemStopScreenMirror",
        "systemRequestScreenMirrorAudio",
        "systemRequestScreenMirrorKeyFrame",
        "systemUpdateScreenMirrorQuality" -> SystemScreenMirrorHost.handle(method, params)
        "systemImageSearchStatus",
        "systemEnableImageSearch",
        "systemDisableImageSearch",
        "systemCancelImageModelDownload",
        "systemStartImageIndex",
        "systemCancelImageIndex" -> SystemImageSearchHost.handle(method, params)
        "zipItemsFacts",
        "scanFilesFacts",
        "fileMetadataFacts",
        "systemMountFacts",
        "systemRecentFileFacts",
        "systemImageFileInfo",
        "systemVideoFileInfo",
        "systemAudioFileInfo",
        "systemDeleteFiles",
        "systemCreateDir",
        "systemRenameFile",
        "systemWriteTextFile",
        "systemTransferFile" -> SystemFilesHost.handle(method, params)
        "systemMediaBucketItemFacts",
        "systemMediaRows",
        "systemImageRows",
        "systemMediaCount",
        "systemImageCount",
        "systemDocExtGroups",
        "systemMediaAction" -> SystemMediaHost.handle(method, params)
        "systemMediaTagFacts",
        "systemTagQueryStubs",
        "systemTagQueryKeys" -> SystemTagsHost.handle(method, params)
        "systemChatChannelFacts" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.channel.ChannelCacher.channels.value
                .sortedBy { it.name }
                .map { channelFacts(it) })
        "systemChatChannelAction" -> chatChannelAction(params)
        "systemChatSend",
        "systemChatDeleteOne",
        "systemChatDeleteQuery",
        "systemChatRetry" -> SystemChatsHost.handle(method, params)
        "systemPeerFacts",
        "systemDeletePeer",
        "systemUnpairPeer",
        "systemPairDevice",
        "systemCancelPairing",
        "systemRespondToPairing" -> SystemPairingHost.handle(method, params)
        "systemAudioPlaylistTracks",
        "systemAudioSearchTracks",
        "systemAudioLyrics",
        "systemAudioPlaybackState",
        "systemAudioPlayMode",
        "systemAudioLibrarySort",
        "systemAudioPlay",
        "systemAudioClear" -> SystemAudioHost.handle(method, params)
        "systemDbFacts",
        "systemDbRowCount",
        "systemDbRows",
        "systemDbColumns",
        "systemDbInfo",
        "systemCreateDbRow",
        "systemDeleteDbRows" -> SystemDatabaseHost.handle(method, params)
        "uploadTmpDirFacts",
        "systemUploadedChunkFacts",
        "systemMergeStatusFacts",
        "systemDeleteChunks",
        "systemMergeChunks",
        "systemMergeAppFileChunks" -> SystemUploadsHost.handle(method, params)
        "systemStopDiscovery",
        "systemStartDiscovery",
        "systemDiscoveryFacts" -> SystemDiscoveryHost.handle(method, params)
        "systemDeleteRecords" -> SystemRecordsHost.handle(method, params)
        "systemSetClipboard",
        "systemEpochMillis",
        "systemPermissionFacts",
        "systemOpenAccessibilitySettings",
        "systemOpenWebSettings",
        "systemSetUserPref",
        "systemRemoveUserPref",
        "systemDeviceInfoFacts",
        "systemDeviceStatusFacts",
        "systemAppFacts",
        "systemAppLogFacts",
        "systemClearAppLogs",
        "systemSetTempValue",
        "systemRelaunchApp",
        "systemUpdateDeviceName" -> SystemAppHost.handle(method, params)
        else -> error("Unsupported provider operation")
    }
}

private suspend fun chatChannelAction(params: JsonObject): JsonElement {
    val manager = com.ismartcoding.plain.chat.channel.ChannelManager
    val id = params["id"]?.jsonPrimitive?.content.orEmpty()
    val peer = params["peerId"]?.jsonPrimitive?.content.orEmpty()
    return when (params.getValue("action").jsonPrimitive.content) {
        "create" -> JsonHelper.jsonEncodeToElement(
            manager.createChannel(params.getValue("name").jsonPrimitive.content))
        "rename" -> JsonHelper.jsonEncodeToElement(manager.renameChannel(
            id, params.getValue("name").jsonPrimitive.content))
        "delete" -> { manager.deleteChannel(id); JsonHelper.jsonEncodeToElement(true) }
        "leave" -> { manager.leaveChannel(id); JsonHelper.jsonEncodeToElement(true) }
        "invite" -> JsonHelper.jsonEncodeToElement(manager.inviteMember(id, peer))
        "kick" -> JsonHelper.jsonEncodeToElement(manager.kickMember(id, peer))
        "accept" -> { manager.acceptInvite(id); JsonHelper.jsonEncodeToElement(true) }
        "decline" -> { manager.declineInvite(id); JsonHelper.jsonEncodeToElement(true) }
        else -> error("Unsupported channel action")
    }
}
