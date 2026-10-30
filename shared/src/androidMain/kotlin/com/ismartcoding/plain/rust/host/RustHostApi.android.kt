package com.ismartcoding.plain.rust.host

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.NetworkMonitor
import com.ismartcoding.plain.features.contacts.ContactInput
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.contacts.GroupHelper
import com.ismartcoding.plain.platform.contacts.SourceHelper
import com.ismartcoding.plain.platform.media.countMedia
import com.ismartcoding.plain.platform.media.getMediaIds
import com.ismartcoding.plain.platform.media.provider.ContactMediaStoreHelper
import com.ismartcoding.plain.platform.notifications.cancelNotificationFacts
import com.ismartcoding.plain.platform.notifications.notificationFacts
import com.ismartcoding.plain.platform.notifications.replyNotification
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.platform.telephony.getSims
import com.ismartcoding.plain.rust.host.provider.ContactPageFacts
import com.ismartcoding.plain.rust.host.provider.ProviderPageRequest
import com.ismartcoding.plain.rust.host.provider.providerPlan
import com.ismartcoding.plain.platform.files.handleMediaProvider
import com.ismartcoding.plain.platform.media.audioLibraryResolve
import com.ismartcoding.plain.platform.system.deleteSystemProviderFacts
import com.ismartcoding.plain.rust.host.files.FileTaskHost
import com.ismartcoding.plain.rust.host.files.SystemFilesHost
import com.ismartcoding.plain.rust.host.network.PeerAwareHost
import com.ismartcoding.plain.rust.host.provider.SystemCallsHost
import com.ismartcoding.plain.rust.host.provider.SystemMediaHost
import com.ismartcoding.plain.rust.host.provider.SystemPackagesHost
import com.ismartcoding.plain.rust.host.provider.SystemScreenMirrorHost
import com.ismartcoding.plain.rust.host.provider.SystemSmsHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

internal actual suspend fun executePlatformHostRequest(method: String, params: JsonObject, scope: CoroutineScope): HostResult = when (method) {
    "systemNetworkInterfaceFacts" -> hostResult(NetworkMonitor.interfaceFacts())
    "peerStartAware", "peerStopAware", "peerTransportSocketOpen", "peerTransportSocketClose",
    "peerTransportSocketRead", "peerTransportSocketWrite", "peerTransportSocketCloseAll" ->
        PeerAwareHost.handle(method, params, scope)
    "systemPackageFacts", "systemPackageStatuses", "systemInstallPackage", "systemPackageInstallProfile",
    "systemUninstallPackages" -> SystemPackagesHost.handle(method, params)
    "systemCallFacts", "systemCallCount", "systemCallIds", "systemMakeCall" -> SystemCallsHost.handle(method, params)
    "systemMmsTextFacts", "systemSmsCountFacts", "systemSmsRowsFacts", "systemSmsIdsFacts",
    "systemSmsConversationFacts", "systemSmsThreadFacts", "systemDivideSmsText", "systemSendSmsParts",
    "systemDeleteSmsRows", "systemMmsLatest", "systemMmsLaunch", "systemMmsCandidates" -> SystemSmsHost.handle(method, params)
    "systemScreenMirrorControls", "systemScreenMirrorResetTouch", "systemScreenMirrorState",
    "systemStartScreenMirror", "systemStopScreenMirror", "systemRequestScreenMirrorAudio",
    "systemRequestScreenMirrorKeyFrame", "systemOpenAccessibilitySettings" -> SystemScreenMirrorHost.handle(method, params)
    "systemFileIndexId", "systemFileIndexRows", "systemFileIndexCount" -> SystemFilesHost.handle(method, params)
    "systemMediaBucketItemFacts", "systemMediaRows", "systemMediaCount", "systemMediaIdsFacts" -> SystemMediaHost.handle(method, params)
    "fileTaskScan", "fileTaskMediaSnapshot" -> FileTaskHost.handle(method, params)
    "mediaProviderRows", "mediaProviderWrite" -> hostResult(handleMediaProvider(method, params))
    "audioLibraryResolve" -> {
        val request = JsonHelper.jsonDecodeFromElement<AudioLibraryResolveRequest>(params)
        hostResult(audioLibraryResolve(request.ids, request.paths))
    }
    "systemDeleteRecords" -> {
        val request = JsonHelper.jsonDecodeFromElement<DeleteRecordsRequest>(params)
        hostResult(deleteSystemProviderFacts(DataType.valueOf(request.provider), request.ids.toSet()))
    }
    "systemSimFacts" -> hostResult(getSims())
    "systemContactFacts" -> {
        val request = JsonHelper.jsonDecodeFromElement<ProviderPageRequest>(params)
        val page = if (Permission.READ_CONTACTS.isGranted()) {
            ContactMediaStoreHelper.searchAsync(appContext, request.plan.where(), request.limit, request.offset)
        } else ContactPageFacts()
        hostResult(page)
    }
    "systemContactCount" -> hostResult(countMedia(DataType.CONTACT, params.providerPlan()))
    "systemContactIds" -> hostResult(getMediaIds(DataType.CONTACT, params.providerPlan()))
    "systemContactGroupFacts" -> hostResult(GroupHelper.getAll())
    "systemContactSources" -> hostResult(SourceHelper.getAll())
    "systemCreateContact" -> {
        val request = JsonHelper.jsonDecodeFromElement<CreateContactRequest>(params)
        hostResult(ContactMediaStoreHelper.createAsync(request.input, request.accountType))
    }
    "systemUpdateContact" -> {
        val request = JsonHelper.jsonDecodeFromElement<UpdateContactRequest>(params)
        ContactMediaStoreHelper.updateAsync(request.id, request.input)
        hostResult(true)
    }
    "systemCreateContactGroup" -> {
        val request = JsonHelper.jsonDecodeFromElement<CreateContactGroupRequest>(params)
        hostResult(GroupHelper.create(request.name, request.accountName, request.accountType).id)
    }
    "systemUpdateContactGroup" -> {
        val request = JsonHelper.jsonDecodeFromElement<UpdateContactGroupRequest>(params)
        GroupHelper.update(request.id, request.name)
        hostResult(true)
    }
    "systemDeleteContactGroup" -> {
        GroupHelper.delete(JsonHelper.jsonDecodeFromElement<IdRequest>(params).id)
        hostResult(true)
    }
    "systemNotificationFacts" -> hostResult(notificationFacts())
    "systemCancelNotifications" -> hostResult(cancelNotificationFacts(
        JsonHelper.jsonDecodeFromElement<IdsRequest>(params).ids.toSet()))
    "systemReplyNotification" -> {
        val request = JsonHelper.jsonDecodeFromElement<ReplyNotificationRequest>(params)
        hostResult(replyNotification(request.id, request.actionIndex, request.text))
    }
    else -> error("Unknown Rust host operation: $method")
}

@Serializable
private data class CreateContactRequest(val input: ContactInput, val accountType: String)

@Serializable
private data class UpdateContactRequest(val id: String, val input: ContactInput)

@Serializable
private data class CreateContactGroupRequest(val name: String, val accountName: String, val accountType: String)

@Serializable
private data class UpdateContactGroupRequest(val id: String, val name: String)

@Serializable
private data class ReplyNotificationRequest(val id: String, val actionIndex: Int, val text: String)

@Serializable
private data class DeleteRecordsRequest(val provider: String, val ids: List<String>)

@Serializable
private data class AudioLibraryResolveRequest(val ids: List<String>, val paths: List<String>)
