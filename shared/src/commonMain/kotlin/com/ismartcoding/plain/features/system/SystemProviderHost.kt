package com.ismartcoding.plain.features.system

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
        "systemPhoneLocaleFacts",
        "systemPhoneMetadata",
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
        "systemMmsLatest", "systemMmsLaunch", "systemMmsCandidates",
        "systemSimFacts" -> SystemSmsHost.handle(method, params)
        "systemWebLoginRequest",
        "systemWebLoginCompleted",
        "systemWebSocketRegistered",
        "systemScreenMirrorControls",
        "systemScreenMirrorResetTouch" -> com.ismartcoding.plain.platform.WebSocketHost.handle(method, params)
        "systemScreenMirrorState",
        "systemStartScreenMirror",
        "systemStopScreenMirror",
        "systemRequestScreenMirrorAudio",
        "systemRequestScreenMirrorKeyFrame",
        "systemUpdateScreenMirrorQuality" -> SystemScreenMirrorHost.handle(method, params)
        "systemImageModelsObserve" -> SystemImageSearchHost.handle(method, params)
        "systemCastAddressFacts" -> com.ismartcoding.plain.platform.CastHost.handle(method, params)
        "systemFileResource" -> com.ismartcoding.plain.platform.FileResourceHost.handle(params)
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
        "systemImageIdsFacts",
        "systemMediaCount",
        "systemDocExtGroups",
        "systemMediaAction" -> SystemMediaHost.handle(method, params)
        "systemMediaTagFacts",
        "systemTagQueryStubs",
        "systemTagQueryKeys" -> SystemTagsHost.handle(method, params)
        "systemAudioPlaylistTracks",
        "systemAudioSearchTracks",
        "systemAudioLyrics",
        "systemAudioPlaybackState",
        "systemAudioPlayMode",
        "systemAudioLibrarySort",
        "systemAudioPlay",
        "systemAudioClear" -> SystemAudioHost.handle(method, params)
        "systemDbPath" -> SystemDatabaseHost.handle(method, params)
        "uploadTmpDirFacts" -> SystemUploadsHost.handle(method, params)
        "systemStopDiscovery",
        "systemStartDiscovery",
        "systemDiscoveryFacts" -> SystemDiscoveryHost.handle(method, params)
        "systemDeleteRecords" -> SystemRecordsHost.handle(method, params)
        "systemSetClipboard",
        "systemEpochMillis",
        "systemPermissionFacts",
        "systemOpenAccessibilitySettings",
        "systemOpenWebSettings",
        "systemDeviceInfoFacts",
        "systemDeviceStatusFacts",
        "systemAppFacts",
        "systemAppLogFacts",
        "systemClearAppLogs",
        "systemSetTempValue",
        "systemRelaunchApp" -> SystemAppHost.handle(method, params)
        else -> error("Unsupported provider operation")
    }
}
