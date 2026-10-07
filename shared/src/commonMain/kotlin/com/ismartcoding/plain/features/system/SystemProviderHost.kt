package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.chat.channel.channelFacts
import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.enums.WebSettingsFeature
import com.ismartcoding.plain.httpserver.MergeClaim
import com.ismartcoding.plain.httpserver.MergeJobs
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.httpserver.sendMms
import com.ismartcoding.plain.data.getGeo
import com.ismartcoding.plain.extensions.parseEpochMillis
import com.ismartcoding.plain.features.contact.DContentItem
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.pinyin.Pinyin
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.installedPackageFacts
import com.ismartcoding.plain.platform.notificationFacts
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.enums.has
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private fun contactInput(params: JsonObject): com.ismartcoding.plain.httpserver.models.ContactInput =
    JsonHelper.jsonDecodeFromElement(params.getValue("input"))

/**
 * The public contact contract, field for field. The `ContactsContract`
 * `DATA2` code stays a raw int on each detail row: Rust owns the enum, so the
 * wire keeps the number and the mapping stays in one place.
 */
private fun contactFacts(contact: DContact): ContactFacts {
    fun contentItem(item: DContentItem): ContactDetailFacts = ContactDetailFacts(
        value = item.value,
        type = item.type,
        label = item.label,
    )
    fun contentItems(items: List<DContentItem>): List<ContactDetailFacts> = items.map(::contentItem)
    return ContactFacts(
        id = contact.id,
        prefix = contact.prefix,
        firstName = contact.givenName,
        middleName = contact.middleName,
        lastName = contact.familyName,
        suffix = contact.suffix,
        nickname = contact.nickname,
        photoId = contact.photoUri,
        phoneNumbers = contact.phoneNumbers.map { phone ->
            ContactPhoneFacts(
                value = phone.value,
                type = phone.type,
                label = phone.label,
                normalizedNumber = phone.normalizedNumber,
            )
        },
        emails = contentItems(contact.emails),
        addresses = contentItems(contact.addresses),
        events = contentItems(contact.events),
        websites = contentItems(contact.websites),
        ims = contentItems(contact.ims),
        source = contact.source,
        starred = contact.starred != 0,
        contactId = contact.contactId,
        thumbnailId = contact.thumbnailUri,
        notes = contact.notes,
        groups = contact.groups.map { group ->
            ContactGroupFacts(
                id = group.id.toString(),
                name = group.name,
            )
        },
        organization = contact.organization?.let { organization ->
            ContactOrganizationFacts(
                company = organization.company,
                title = organization.title,
            )
        },
        ringtone = contact.ringtone,
        updatedAt = contact.updatedAt.toString(),
    )
}

/** The public call-log contract; the geo lookup stays on the platform. */
private fun callFacts(call: DCall): CallFacts {
    val geo = call.getGeo()
    return CallFacts(
        id = call.id,
        number = call.number,
        name = call.name,
        photoId = com.ismartcoding.plain.helpers.getFileId(call.photoUri),
        startedAt = call.startedAt.toString(),
        durationSec = call.durationSec,
        type = call.type,
        accountId = call.accountId,
        geo = geo?.let { value ->
            PhoneGeoFacts(
                country = value.country,
                numberType = value.numberType,
                carrier = value.carrier,
                description = value.description,
            )
        },
    )
}

object SystemProviderHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemPackageFacts" -> JsonHelper.jsonEncodeToElement(installedPackageFacts().map { item ->
            PackageFacts(
                item = item,
                nameSortKey = Pinyin.toPinyin(item.name).lowercase(),
            )
        })
        "systemPackageStatuses" -> {
            val ids = params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }
            JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getPackageInfoMap(ids))
        }
        "systemInstallPackage" -> {
            val path = params.getValue("path").jsonPrimitive.content
            val result = try {
                com.ismartcoding.plain.platform.installPackage(path)
            } catch (e: Exception) {
                throw IllegalStateException("Installation failed: ${e.message}", e)
            }
            JsonHelper.jsonEncodeToElement(PackageInstallFacts(
                packageName = result.packageName,
                lastUpdateTime = result.lastUpdateTime?.toString(),
                isNew = result.isNew,
            ))
        }
        "systemUninstallPackages" -> {
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.forEach {
                com.ismartcoding.plain.platform.uninstallPackage(it)
            }
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemSetClipboard" -> {
            com.ismartcoding.plain.platform.setClipboardText("plain", params.getValue("text").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemNotificationFacts" -> JsonHelper.jsonEncodeToElement(notificationFacts())
        "systemMmsTextFacts", "systemSmsCountFacts", "systemSmsRowsFacts", "systemSmsIdsFacts", "systemSmsConversationFacts", "systemSmsThreadFacts" -> com.ismartcoding.plain.platform.systemSmsFacts(method, params)
        "systemEpochMillis" -> JsonHelper.jsonEncodeToElement(params.getValue("values").jsonArray.associate { value ->
            val text = value.jsonPrimitive.content
            text to text.parseEpochMillis()
        })
        "systemPermissionFacts" -> run {
            val granted = params.getValue("permissions").jsonArray.associate { item ->
                val name = item.jsonPrimitive.content
                name to com.ismartcoding.plain.platform.Permission.valueOf(name).isGranted()
            }
            JsonHelper.jsonEncodeToElement(PermissionFacts(
                granted = granted,
            ))
        }
        "systemSendSms" -> {
            com.ismartcoding.plain.platform.sendSmsText(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("body").jsonPrimitive.content,
                params["subscriptionId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.int,
                params["clientId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                params["clientRequestId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "zipItemsFacts" -> {
            val type = params.getValue("type").jsonPrimitive.content
            val items = com.ismartcoding.plain.platform.searchZipItems(
                type,
                params.getValue("query").jsonPrimitive.content,
                params.getValue("id").jsonPrimitive.content,
            ).filter { com.ismartcoding.plain.platform.fileExists(it.sourcePath) }
            JsonHelper.jsonEncodeToElement(ZipItemsFacts(
                items = items.map { entry ->
                    ZipEntryFacts(
                        path = entry.sourcePath,
                        name = entry.entryName,
                    )
                },
            ))
        }
        "uploadTmpDirFacts" -> JsonHelper.jsonEncodeToElement(PathFacts(
            path = com.ismartcoding.plain.platform.getUploadTmpDirPath(),
        ))
        "scanFilesFacts" -> {
            com.ismartcoding.plain.platform.scanFiles(
                params.getValue("paths").jsonArray.map { it.jsonPrimitive.content }.toTypedArray()
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemContactFacts" -> {
            val query = params.getValue("query").jsonPrimitive.content
            val offset = params.getValue("offset").jsonPrimitive.int
            val limit = params.getValue("limit").jsonPrimitive.int
            JsonHelper.jsonEncodeToElement(
                com.ismartcoding.plain.platform.searchMedia(
                    com.ismartcoding.plain.enums.DataType.CONTACT, query, limit, offset,
                    com.ismartcoding.plain.features.file.FileSortBy.DATE_DESC,
                ).filterIsInstance<DContact>().map(::contactFacts)
            )
        }
        "systemContactCount" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.countMedia(
                com.ismartcoding.plain.enums.DataType.CONTACT,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemContactIds" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getMediaIds(
                com.ismartcoding.plain.enums.DataType.CONTACT,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemContactGroupFacts" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getContactGroups().map { group ->
                ContactGroupFacts(
                    id = group.id.toString(),
                    name = group.name,
                )
            }
        )
        "systemContactSources" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getContactSources().map { source ->
                ContactSourceFacts(
                    name = source.name,
                    type = source.type,
                )
            }
        )
        "systemCallFacts" -> {
            val query = params.getValue("query").jsonPrimitive.content
            val offset = params.getValue("offset").jsonPrimitive.int
            val limit = params.getValue("limit").jsonPrimitive.int
            JsonHelper.jsonEncodeToElement(
                com.ismartcoding.plain.platform.searchMedia(
                    com.ismartcoding.plain.enums.DataType.CALL, query, limit, offset,
                    com.ismartcoding.plain.features.file.FileSortBy.DATE_DESC,
                ).filterIsInstance<DCall>().map(::callFacts)
            )
        }
        "systemCallCount" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.countMedia(
                com.ismartcoding.plain.enums.DataType.CALL,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemCallIds" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getMediaIds(
                com.ismartcoding.plain.enums.DataType.CALL,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemMakeCall" -> {
            com.ismartcoding.plain.platform.call(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("showDialer").jsonPrimitive.boolean,
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemTrashSms" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.trashSms(params.getValue("query").jsonPrimitive.content)
        )
        "systemRestoreSms" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.restoreSms(params.getValue("query").jsonPrimitive.content)
        )
        "systemDeleteSms" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.deleteSms(params.getValue("query").jsonPrimitive.content)
        )
        "systemSendMms" -> JsonHelper.jsonEncodeToElement(
            sendMms(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("body").jsonPrimitive.content,
                params.getValue("attachmentPaths").jsonArray.map { it.jsonPrimitive.content },
                com.ismartcoding.plain.httpserver.models.ID(params.getValue("threadId").jsonPrimitive.content),
            )
        )
        "systemScreenMirrorState" -> {
            val codec = com.ismartcoding.plain.platform.getScreenMirrorVideoCodec()
            JsonHelper.jsonEncodeToElement(ScreenMirrorStateFacts(
                running = com.ismartcoding.plain.platform.isScreenMirrorRunning(),
                controlEnabled = com.ismartcoding.plain.platform.isScreenMirrorControlEnabled(),
                codec = codec?.let { value ->
                    ScreenMirrorCodecFacts(
                        annexB = value.annexB,
                        keyFrame = value.keyFrame,
                    )
                },
            ))
        }
        "systemScreenMirrorQuality" -> JsonHelper.jsonEncodeToElement(UserPrefs.screenMirrorQualityValue().toModel())
        "systemStartScreenMirror" -> {
            com.ismartcoding.plain.platform.applyScreenMirrorQualityPreference()
            sendEvent(
                com.ismartcoding.plain.events.HStartScreenMirrorEvent(
                    params.getValue("audio").jsonPrimitive.boolean
                )
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemStopScreenMirror" -> {
            com.ismartcoding.plain.platform.stopScreenMirror()
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemRequestScreenMirrorAudio" -> {
            val granted = com.ismartcoding.plain.platform.Permission.RECORD_AUDIO.isGranted()
            if (!granted) {
                sendEvent(
                    com.ismartcoding.plain.events.HRequestScreenMirrorAudioEvent()
                )
            }
            JsonHelper.jsonEncodeToElement(granted)
        }
        "systemRequestScreenMirrorKeyFrame" -> {
            com.ismartcoding.plain.platform.requestScreenMirrorKeyFrame()
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemUpdateScreenMirrorQuality" -> {
            val mode = when (params.getValue("mode").jsonPrimitive.content) {
                "SMOOTH" -> com.ismartcoding.plain.enums.ScreenMirrorMode.SMOOTH
                else -> com.ismartcoding.plain.enums.ScreenMirrorMode.HD
            }
            val quality = DScreenMirrorQuality(
                mode, if (mode == com.ismartcoding.plain.enums.ScreenMirrorMode.SMOOTH) 720 else 1080
            )
            com.ismartcoding.plain.preferences.UserPrefs.setScreenMirrorQuality(quality)
            com.ismartcoding.plain.platform.onScreenMirrorQualityChanged(mode)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemOpenAccessibilitySettings" -> {
            sendEvent(com.ismartcoding.plain.events.HOpenAccessibilitySettingsEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemOpenWebSettings" -> {
            val feature = params["feature"]
            ?.takeUnless { it is JsonNull }
            ?.jsonPrimitive
            ?.content
            ?.let { name ->
                WebSettingsFeature.entries
                .firstOrNull { it.name == name }
            }
            sendEvent(com.ismartcoding.plain.events.HOpenWebSettingsEvent(feature))
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemImageSearchStatus" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.buildImageSearchStatus())
        "systemEnableImageSearch" -> {
            sendEvent(com.ismartcoding.plain.events.HEnableImageSearchEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemDisableImageSearch" -> {
            sendEvent(com.ismartcoding.plain.events.HDisableImageSearchEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemCancelImageModelDownload" -> {
            sendEvent(com.ismartcoding.plain.events.HCancelImageModelDownloadEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemStartImageIndex" -> {
            com.ismartcoding.plain.platform.startImageIndexFullScan(
                params.getValue("force").jsonPrimitive.boolean
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemCancelImageIndex" -> {
            com.ismartcoding.plain.platform.cancelImageIndex()
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemDeleteRecords" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.deleteSystemProviderFacts(
            com.ismartcoding.plain.enums.DataType.valueOf(params.getValue("provider").jsonPrimitive.content),
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()))
        "systemCreateContact" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.createContact(
            contactInput(params)))
        "systemUpdateContact" -> {
            com.ismartcoding.plain.platform.updateContact(
                params.getValue("id").jsonPrimitive.content,
                contactInput(params),
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemCreateContactGroup" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.createContactGroup(
            params.getValue("name").jsonPrimitive.content,
            params.getValue("accountName").jsonPrimitive.content,
            params.getValue("accountType").jsonPrimitive.content,
            ).id.toString())
        "systemUpdateContactGroup" -> {
            com.ismartcoding.plain.platform.updateContactGroup(
                params.getValue("id").jsonPrimitive.content,
                params.getValue("name").jsonPrimitive.content,
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemDeleteContactGroup" -> {
            com.ismartcoding.plain.platform.deleteContactGroup(params.getValue("id").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemCancelNotifications" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.cancelNotificationFacts(
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()))
        "systemReplyNotification" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.replyNotification(
            params.getValue("id").jsonPrimitive.content, params.getValue("actionIndex").jsonPrimitive.int, params.getValue("text").jsonPrimitive.content))
        "fileMetadataFacts" -> {
            val path = params.getValue("path").jsonPrimitive.content
            JsonHelper.jsonEncodeToElement(FileMetadataFacts(
                size = com.ismartcoding.plain.platform.statFile(path)?.size ?: 0,
                mimeType = com.ismartcoding.plain.platform.getContentTypeForPath(path).orEmpty(),
            ))
        }
        "systemMountFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.httpserver.loaders.MountsLoader.load().map { mount ->
            MountFacts(
                id = mount.id.value,
                name = mount.name,
                path = mount.path,
                mountPoint = mount.mountPoint,
                fsType = mount.fsType,
                totalBytes = mount.totalBytes,
                usedBytes = mount.usedBytes,
                freeBytes = mount.freeBytes,
                remote = mount.remote,
                alias = mount.alias,
                driveType = mount.driveType.name,
                diskId = mount.diskId,
            )
        })
        "systemRecentFileFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getRecentFiles().map(::fileFacts))
        "systemImageFileInfo" -> fileInfoFacts(method, params)
        "systemVideoFileInfo" -> fileInfoFacts(method, params)
        "systemAudioFileInfo" -> fileInfoFacts(method, params)
        "systemMediaBucketItemFacts" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.mediaBucketItemFacts(mediaDataType(params)).map { item ->
                MediaBucketItemFacts(
                    id = item.id,
                    name = item.name,
                    size = item.size,
                    path = item.path,
                    sortName = item.sortName,
                )
        })
        "systemMediaRows" -> {
            val items = com.ismartcoding.plain.platform.searchMedia(
                mediaDataType(params),
                params.getValue("query").jsonPrimitive.content,
                params.getValue("limit").jsonPrimitive.int,
                params.getValue("offset").jsonPrimitive.int,
                fileSortBy(params),
            )
            JsonHelper.jsonEncodeToElement(items.map { mediaFacts(it) })
        }
        "systemImageRows" -> {
            val items = com.ismartcoding.plain.platform.searchImagesCombined(
                params.getValue("queryText").jsonPrimitive.content,
                params.getValue("extraQuery").jsonPrimitive.content,
                params.getValue("limit").jsonPrimitive.int,
                params.getValue("offset").jsonPrimitive.int,
                fileSortBy(params),
            )
            JsonHelper.jsonEncodeToElement(items.map { mediaFacts(it) })
        }
        "systemMediaCount" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.countMedia(
            mediaDataType(params), params.getValue("query").jsonPrimitive.content))
        "systemImageCount" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.countImagesCombined(
            params.getValue("queryText").jsonPrimitive.content,
            params.getValue("extraQuery").jsonPrimitive.content))
        "systemDocExtGroups" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getDocExtGroups("").map { (ext, count) ->
                DocExtensionFacts(
                    ext = ext,
                    count = count,
                ) })
        "systemMediaTagFacts" -> mediaTagFacts(params)
        "systemChatChannelFacts" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.channel.ChannelCacher.channels.value
                .sortedBy { it.name }
                .map { channelFacts(it) })
        "systemChatChannelAction" -> chatChannelAction(params)
        "systemChatSend" -> {
            val item = com.ismartcoding.plain.chat.ChatManager.sendContent(
                com.ismartcoding.plain.chat.data.ChatTarget.parseId(
                    params.getValue("target").jsonPrimitive.content),
                com.ismartcoding.plain.db.DChat.parseContent(
                    params.getValue("content").jsonPrimitive.content))
            com.ismartcoding.plain.chat.ChatViewModel.onMessagesCreated(
                com.ismartcoding.plain.chat.data.ChatTarget.parseId(
                    params.getValue("target").jsonPrimitive.content), listOf(item))
            JsonHelper.jsonEncodeToElement(listOf(item))
        }
        "systemChatDeleteOne" -> {
            val id = params.getValue("id").jsonPrimitive.content
            com.ismartcoding.plain.chat.ChatManager.getChatItem(id)?.let {
                com.ismartcoding.plain.chat.ChatManager.deleteOne(it.id)
                com.ismartcoding.plain.chat.ChatViewModel.onMessagesDeleted(setOf(it.id))
            }
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemChatDeleteQuery" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.ChatManager.deleteQuery(
                params.getValue("query").jsonPrimitive.content))
        "systemChatRetry" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.ChatManager.retry(
                params.getValue("id").jsonPrimitive.content))
        "systemPeerFacts" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.peer.PeerCacher.peersMap.value.values
            .map { it.peer })
        "systemDeletePeer" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.peer.PeerManager.deletePeer(
                params.getValue("id").jsonPrimitive.content))
        "systemUnpairPeer" -> {
            com.ismartcoding.plain.ui.models.NearbyViewModel.unpairDevice(
                params.getValue("id").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemPairDevice" -> {
            val input = params.getValue("input").jsonObject
            com.ismartcoding.plain.ui.models.NearbyViewModel.startPairing(nearbyDevice(input))
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemCancelPairing" -> {
            com.ismartcoding.plain.ui.models.NearbyViewModel.cancelPairing(
                params.getValue("deviceId").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemRespondToPairing" -> {
            com.ismartcoding.plain.discover.PairingResponder.respond(
                pairingRequest(params.getValue("input").jsonObject),
                params.getValue("accepted").jsonPrimitive.boolean)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemSimFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getSims().map { sim ->
            SimFacts(
                id = sim.id,
                label = sim.label,
                number = sim.number,
                subscriptionId = sim.subscriptionId,
            )
        })

        // One playlist row per path: the metadata is read off the file, which
        // is the platform's job — Rust stores whatever this reports.
        "systemAudioPlaylistTracks" -> JsonHelper.jsonEncodeToElement(
            params.getValue("paths").jsonArray.map { value ->
                val track = com.ismartcoding.plain.platform.playlistAudioFromPath(
                    value.jsonPrimitive.content)
                playlistTrackFacts(track)
        })
        "systemAudioSearchTracks" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.searchMedia(
                com.ismartcoding.plain.enums.DataType.AUDIO,
                params.getValue("query").jsonPrimitive.content,
                params.getValue("limit").jsonPrimitive.int,
                params.getValue("offset").jsonPrimitive.int,
                com.ismartcoding.plain.features.file.FileSortBy.valueOf(
                    params.getValue("sortBy").jsonPrimitive.content),
            ).filterIsInstance<com.ismartcoding.plain.audio.DAudio>()
            .map { playlistTrackFacts(com.ismartcoding.plain.audio.DPlaylistAudio(
                title = it.title, path = it.path, artist = it.artist,
                durationMs = it.durationMs)) })
        "systemAudioLyrics" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getAudioLyrics(
                params.getValue("path").jsonPrimitive.content))
        "systemAudioPlaybackState" -> JsonHelper.jsonEncodeToElement(AudioPlaybackFacts(
            isPlaying = com.ismartcoding.plain.platform.audioIsPlayingFlow().value,
            positionMs = com.ismartcoding.plain.platform.audioPlayerProgressAsync(),
        ))
        "systemAudioPlayMode" -> when (val mode = params["mode"]) {
            null -> JsonHelper.jsonEncodeToElement(UserPrefs.audioPlayMode.value.name)
            else -> {
                UserPrefs.audioPlayMode.value = com.ismartcoding.plain.enums.MediaPlayMode
                .valueOf(mode.jsonPrimitive.content)
                JsonHelper.jsonEncodeToElement(UserPrefs.audioPlayMode.value.name)
            }
        }
        "systemAudioLibrarySort" -> JsonHelper.jsonEncodeToElement(UserPrefs.audioSortByValue().name)
        "systemAudioPlay" -> {
            com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck(
                playlistTrack(params.getValue("track").jsonObject))
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemAudioClear" -> {
            com.ismartcoding.plain.platform.audioClear()
            com.ismartcoding.plain.lib.sendEvent(
                com.ismartcoding.plain.events.ClearAudioQueueEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemTagQueryStubs" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getMediaTagRelationStubs(
                mediaDataType(params), params.getValue("query").jsonPrimitive.content).map { stub ->
                TagQueryStubFacts(
                    key = stub.key,
                    title = stub.title,
                    size = stub.size,
                )
        })
        "systemTagQueryKeys" -> JsonHelper.jsonEncodeToElement(IdsFacts(
            ids = com.ismartcoding.plain.platform.getMediaIds(
                mediaDataType(params), params.getValue("query").jsonPrimitive.content).toList(),
        ))
        // Pref writes go through here rather than straight to the store:
        // `Prefs` keeps an in-memory copy and fans the change out to the
        // live flows the UI collects, so a write that skipped it would
        // persist the value and leave every observer on the old one.
        "systemSetUserPref" -> {
            com.ismartcoding.plain.preferences.Prefs.setUserPref(
                params.getValue("key").jsonPrimitive.content, params.getValue("value"))
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemRemoveUserPref" -> {
            com.ismartcoding.plain.preferences.Prefs.removeUserPref(params.getValue("key").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemDbFacts" -> JsonHelper.jsonEncodeToElement(DatabaseFacts(
            path = com.ismartcoding.plain.platform.getDbPath(),
            tables = com.ismartcoding.plain.platform.getDbTableNames(),
        ))
        "systemDbRowCount" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getDbTableRowCount(
            params.getValue("table").jsonPrimitive.content))
        "systemDbRows" -> JsonHelper.jsonEncodeToElement(DatabaseRowsFacts(
            rows = com.ismartcoding.plain.platform.getDbTableRows(
                params.getValue("table").jsonPrimitive.content,
                params.getValue("offset").jsonPrimitive.int,
                params.getValue("limit").jsonPrimitive.int),
        ))
        "systemDbColumns" -> JsonHelper.jsonEncodeToElement(DatabaseColumnsFacts(
            columns = com.ismartcoding.plain.platform.getDbTableColumns(params.getValue("table").jsonPrimitive.content),
        ))
        "systemDbInfo" -> JsonHelper.jsonEncodeToElement(DatabaseInfoFacts(
            idKey = com.ismartcoding.plain.platform.getDbTableInfo(params.getValue("table").jsonPrimitive.content).idKey,
        ))
        "systemCreateDbRow" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.createDbTableRow(
            params.getValue("table").jsonPrimitive.content,
            params.getValue("row").jsonPrimitive.content))
        "systemDeleteDbRows" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.deleteDbTableRows(
            params.getValue("table").jsonPrimitive.content,
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }))
        "systemDeleteFiles" -> JsonHelper.jsonEncodeToElement(params.getValue("paths").jsonArray.count { path ->
            com.ismartcoding.plain.platform.deleteFileOrDir(path.jsonPrimitive.content)
        })
        "systemCreateDir" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.createDirectory(params.getValue("path").jsonPrimitive.content))
        "systemRenameFile" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.renameAndScanFile(
            params.getValue("path").jsonPrimitive.content,
            params.getValue("name").jsonPrimitive.content) != null)
        "systemWriteTextFile" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.writeFileText(
                params.getValue("path").jsonPrimitive.content,
                params.getValue("content").jsonPrimitive.content,
                params.getValue("overwrite").jsonPrimitive.boolean))
        "systemTransferFile" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.features.file.FileTaskHelper.execute(
            com.ismartcoding.plain.features.file.FileTaskType.valueOf(params.getValue("type").jsonPrimitive.content),
            listOf(com.ismartcoding.plain.features.file.FileTaskOp(
                params.getValue("src").jsonPrimitive.content,
                params.getValue("dst").jsonPrimitive.content,
                params.getValue("overwrite").jsonPrimitive.boolean),
            )).status == com.ismartcoding.plain.features.file.FileTaskStatus.DONE)
        "systemUploadedChunkFacts" -> JsonHelper.jsonEncodeToElement(UploadedChunksFacts(
            chunks = com.ismartcoding.plain.platform.listUploadedChunks(
                params.getValue("fileId").jsonPrimitive.content),
        ))
        "systemMergeStatusFacts" -> mergeStatusFacts(params.getValue("fileId").jsonPrimitive.content)
        "systemDeleteChunks" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.deleteUploadedChunks(
            params.getValue("fileId").jsonPrimitive.content))
        "systemMergeChunks" -> startMerge(params, isAppFile = false)
        "systemMergeAppFileChunks" -> startMerge(params, isAppFile = true)
        "systemMediaAction" -> runMediaAction(params)
        "systemStopDiscovery" -> {
            com.ismartcoding.plain.discover.RustMdnsRuntime.control("stop")
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemStartDiscovery" -> {
            com.ismartcoding.plain.discover.RustMdnsRuntime.control("start")
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemDiscoveryFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.discover.RustMdnsRuntime.snapshot())
        "systemDeviceInfoFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getDeviceInfo())
        "systemDeviceStatusFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getDeviceStatus())
        "systemAppFacts" -> appFacts()
        "systemAppLogFacts" -> appLogFacts(params)
        "systemClearAppLogs" -> {
            com.ismartcoding.plain.platform.clearLatestLogFile()
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemSetTempValue" -> {
            com.ismartcoding.plain.helpers.TempHelper.setValue(
                params.getValue("key").jsonPrimitive.content, params.getValue("value").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemRelaunchApp" -> {
            com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.RestartAppEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemUpdateDeviceName" -> {
            val name = params.getValue("name").jsonPrimitive.content
            com.ismartcoding.plain.preferences.UserPrefs.deviceName.value = name
            com.ismartcoding.plain.TempData.deviceName.value = name
            com.ismartcoding.plain.discover.MdnsDiscoverManager.updateAdvertisedService()
            JsonHelper.jsonEncodeToElement(true)
        }
        else -> error("Unsupported provider operation")
    }
}

/** Log lines, newest first. A non-blank `text` filters the whole buffer
 * before paging, so a search cannot be cut short by the page size; Rust
 * passes `pathOnly` when it wants the log file path instead. */
private suspend fun appLogFacts(params: JsonObject): JsonElement {
    if (params["pathOnly"]?.jsonPrimitive?.boolean == true) {
        return JsonHelper.jsonEncodeToElement(AppLogFacts(
            path = com.ismartcoding.plain.platform.getLatestLogFilePath(),
            lines = emptyList(),
        ))
    }
    val offset = params.getValue("offset").jsonPrimitive.int
    val limit = params.getValue("limit").jsonPrimitive.int
    val text = params.getValue("query").jsonPrimitive.content.trim()
    val lines = if (text.isEmpty()) {
        com.ismartcoding.plain.platform.readLogLinesNewestFirst(offset, limit)
    } else {
        com.ismartcoding.plain.platform.readLogLinesNewestFirst(0, Int.MAX_VALUE)
        .filter { it.contains(text, ignoreCase = true) }
        .drop(offset.coerceAtLeast(0))
        .take(limit.coerceAtLeast(0))
    }
    return JsonHelper.jsonEncodeToElement(AppLogFacts(
        path = com.ismartcoding.plain.platform.getLatestLogFilePath(),
        lines = lines,
    ))
}

/** The `App` contract row. Capabilities and permissions are named rather
 * than encoded: both enums are the contract's own, so there is no mapping. */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
private suspend fun appFacts(): JsonElement = JsonHelper.jsonEncodeToElement(AppFacts(
    clientId = com.ismartcoding.plain.TempData.clientId,
    urlToken = kotlin.io.encoding.Base64.encode(com.ismartcoding.plain.TempData.urlToken),
    httpPort = UserPrefs.httpPort.value,
    httpsPort = UserPrefs.httpsPort.value,
    appDir = com.ismartcoding.plain.platform.appDir(),
    deviceName = com.ismartcoding.plain.TempData.deviceName.value,
    deviceType = com.ismartcoding.plain.platform.getDeviceType().name,
    capabilities = com.ismartcoding.plain.platform.getDeviceCapabilities().map { it.name },
    buildChannel = com.ismartcoding.plain.enums.AppChannelType
        .fromString(com.ismartcoding.plain.buildChannel).name,
    permissions = com.ismartcoding.plain.features.getGrantedWebPermissionsAsync().map { it.name },
    downloadsDir = com.ismartcoding.plain.platform.getDownloadsDirPath(),
    developerMode = UserPrefs.developerMode.value,
    debug = com.ismartcoding.plain.platform.isDebugBuild(),
))

/** The `File` contract row. `mediaId` stays an empty string for non-media
 * entries; Rust maps that to `null` rather than carrying the sentinel. */
private fun fileFacts(file: com.ismartcoding.plain.features.file.DFile): FileFacts = FileFacts(
    name = file.name,
    path = file.path,
    mediaId = file.mediaId,
    createdAt = file.createdAt?.toEpochMilliseconds(),
    updatedAt = file.updatedAt.toEpochMilliseconds(),
    size = file.size,
    isDir = file.isDir,
    childCount = file.childCount,
)

private fun locationFacts(location: com.ismartcoding.plain.httpserver.models.Location?): LocationFacts? =
    location?.let {
        LocationFacts(
            latitude = it.latitude,
            longitude = it.longitude,
        )
    }

private fun fileInfoFacts(method: String, params: JsonObject): JsonElement {
    val path = params.getValue("path").jsonPrimitive.content
    return when (method) {
        "systemImageFileInfo" -> com.ismartcoding.plain.platform.loadImageInfo(path).let {
            JsonHelper.jsonEncodeToElement(ImageInfoFacts(
                width = it.width,
                height = it.height,
                location = locationFacts(it.location),
            ))
        }
        "systemVideoFileInfo" -> com.ismartcoding.plain.platform.loadVideoInfo(path).let {
            JsonHelper.jsonEncodeToElement(VideoInfoFacts(
                width = it.width,
                height = it.height,
                durationMs = it.durationMs,
                location = locationFacts(it.location),
            ))
        }
        else -> com.ismartcoding.plain.platform.loadAudioInfo(path).let {
            JsonHelper.jsonEncodeToElement(AudioInfoFacts(
                durationMs = it.durationMs,
                location = locationFacts(it.location),
            ))
        }
    }
}

private fun mediaDataType(params: JsonObject) =
    com.ismartcoding.plain.enums.DataType.valueOf(params.getValue("dataType").jsonPrimitive.content)

private fun fileSortBy(params: JsonObject) =
    com.ismartcoding.plain.features.file.FileSortBy.valueOf(params.getValue("sortBy").jsonPrimitive.content)

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

private fun nearbyDevice(input: JsonObject): com.ismartcoding.plain.data.DNearbyDevice =
    JsonHelper.jsonDecodeFromElement(input)

private fun pairingRequest(input: JsonObject): com.ismartcoding.plain.data.DPairingRequest =
    JsonHelper.jsonDecodeFromElement(input)

private fun playlistTrack(track: JsonObject): com.ismartcoding.plain.audio.DPlaylistAudio =
    JsonHelper.jsonDecodeFromElement(track)

private fun playlistTrackFacts(track: com.ismartcoding.plain.audio.DPlaylistAudio): PlaylistTrackFacts =
    PlaylistTrackFacts(
    title = track.title,
    artist = track.artist,
    path = track.path,
    durationMs = track.durationMs,
)

private fun mediaFacts(item: com.ismartcoding.plain.db.IData): JsonElement = when (item) {
    is com.ismartcoding.plain.audio.DAudio -> JsonHelper.jsonEncodeToElement(AudioFacts(
        id = item.id,
        title = item.title,
        artist = item.artist,
        path = item.path,
        size = item.size,
        bucketId = item.bucketId,
        durationMs = item.durationMs,
        albumFileId = com.ismartcoding.plain.platform.getAudioAlbumArtFileId(item),
        createdAt = item.createdAt.toString(),
        updatedAt = item.updatedAt.toString(),
        isFavorite = item.isFavorite,
    ))
    is com.ismartcoding.plain.data.DImage -> mediaFacts(
        item.id, item.title, item.path, item.size, item.bucketId, item.createdAt, item.updatedAt,
        durationMs = 0L, takenAt = item.takenAt, isFavorite = item.isFavorite)
    is com.ismartcoding.plain.data.DVideo -> mediaFacts(
        item.id, item.title, item.path, item.size, item.bucketId, item.createdAt, item.updatedAt,
        durationMs = item.durationMs, takenAt = item.takenAt, isFavorite = item.isFavorite)
    is com.ismartcoding.plain.data.DDoc -> mediaFacts(
        item.id, item.title, item.path, item.size, item.bucketId, item.createdAt, item.updatedAt,
        durationMs = item.durationMs, takenAt = null, isFavorite = false)
    else -> error("Unsupported media row ${item::class.simpleName}")
}

private fun mediaFacts(
    id: String,
    title: String,
    path: String,
    size: Long,
    bucketId: String,
    createdAt: kotlin.time.Instant,
    updatedAt: kotlin.time.Instant,
    durationMs: Long,
    takenAt: kotlin.time.Instant?,
    isFavorite: Boolean,
): JsonElement = JsonHelper.jsonEncodeToElement(MediaFacts(
    id = id,
    title = title,
    path = path,
    size = size,
    bucketId = bucketId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    durationMs = durationMs,
    takenAt = takenAt?.let { it.toString() },
    isFavorite = isFavorite,
))

/** One entry per key that actually has relations; keys with none are
 * omitted and Rust defaults them to an empty list, so an explicit empty
 * row would just be noise on the wire. */
private suspend fun mediaTagFacts(params: JsonObject): JsonElement {
    val type = mediaDataType(params)
    val keys = params.getValue("keys").jsonArray.map { it.jsonPrimitive.content }.toSet()
    if (keys.isEmpty()) return JsonHelper.jsonEncodeToElement(emptyList<String>())
    val relations = com.ismartcoding.plain.features.TagHelper
    .getTagRelationsByKeys(keys, type).groupBy { it.key }
    val tags = com.ismartcoding.plain.features.TagHelper.getAll(type).associateBy { it.id }
    return JsonHelper.jsonEncodeToElement(keys.mapNotNull { key ->
        val ids = relations[key]?.map { it.tagId } ?: return@mapNotNull null
        if (ids.isEmpty()) return@mapNotNull null
        MediaTagsFacts(
            key = key,
            tags = ids.mapNotNull { tags[it] }.map { tag ->
                TagFacts(
                    id = tag.id,
                    name = tag.name,
                    count = tag.count,
                )
            },
        )
    })
}

private suspend fun mergeStatusFacts(fileId: String): JsonElement {
    val task = MergeJobs.status(fileId)
    return JsonHelper.jsonEncodeToElement(MergeStatusFacts(
        status = task.status.name,
        value = task.value,
        mergedSize = task.mergedSize,
        error = task.error,
    ))
}

/** Starts the merge and returns immediately. The claim makes a repeated
 * call idempotent, and the job table is what `mergeStatus` polls when the
 * websocket result is lost. */
private suspend fun startMerge(params: JsonObject, isAppFile: Boolean): JsonElement {
    val fileId = params.getValue("fileId").jsonPrimitive.content
    val totalChunks = params.getValue("totalChunks").jsonPrimitive.int
    val totalSize = params.getValue("totalSize").jsonPrimitive.long
    when (val claim = MergeJobs.claim(fileId)) {
        is MergeClaim.AlreadyDone ->
        return mergeStatusFacts(fileId)
        MergeClaim.InProgress ->
        return JsonHelper.jsonEncodeToElement(MergeStartFacts(
            status = "MERGING",
        ))
        MergeClaim.Claimed -> {}
    }
    if (com.ismartcoding.plain.platform.listUploadedChunks(fileId).isEmpty()) {
        MergeJobs.release(fileId)
        error("No chunks found for $fileId")
    }
    com.ismartcoding.plain.lib.ChannelScope().launch {
        val outcome = runCatching {
            if (isAppFile) {
                com.ismartcoding.plain.platform.mergeUploadedChunks(
                    fileId, totalChunks,
                    params.getValue("fileName").jsonPrimitive.content,
                    replace = true, isAppFile = true, totalSize = totalSize,
                )
            } else {
                com.ismartcoding.plain.platform.mergeUploadedChunks(
                    fileId, totalChunks,
                    params.getValue("path").jsonPrimitive.content,
                    params.getValue("replace").jsonPrimitive.boolean,
                    isAppFile = false, totalSize = totalSize,
                )
            }
        }
        val message = if (outcome.isSuccess) {
            val reply = outcome.getOrThrow()
            val idx = reply.lastIndexOf(':')
            val value = if (idx > 0) reply.substring(0, idx) else reply
            val size = if (idx > 0) reply.substring(idx + 1).toLongOrNull() ?: 0L else 0L
            MergeJobs.finish(fileId, value, size)
            com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.WebSocketEvent(
                com.ismartcoding.plain.events.EventType.UPLOAD_MERGE_RESULT,
                JsonHelper.jsonEncode(com.ismartcoding.plain.events.UploadMergeResultData(
                    fileId = fileId, ok = true, value = value, mergedSize = size)),
            ))
            null
        } else {
            val text = outcome.exceptionOrNull()?.message ?: "merge failed"
            MergeJobs.fail(fileId, text)
            com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.WebSocketEvent(
                com.ismartcoding.plain.events.EventType.UPLOAD_MERGE_RESULT,
                JsonHelper.jsonEncode(com.ismartcoding.plain.events.UploadMergeResultData(
                    fileId = fileId, ok = false, error = text)),
            ))
            text
        }
        if (message != null) error(message)
    }
    return JsonHelper.jsonEncodeToElement(MergeStartFacts(
        status = "STARTED",
    ))
}

/** Each action resolves its own id source: a restore looks in the trash,
 * a trash or a move looks in the live library, and only a delete has to
 * ask whether the trash feature is on at all. */
private suspend fun runMediaAction(params: JsonObject): JsonElement {
    val action = params.getValue("action").jsonPrimitive.content
    val type = mediaDataType(params)
    val query = params.getValue("query").jsonPrimitive.content
    val destDir = params["destDir"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: ""
    val fromTrash = action == "delete" && com.ismartcoding.plain.enums.AppFeatureType.MEDIA_TRASH.has()
    val ids = when {
        action == "restore" -> com.ismartcoding.plain.platform.getTrashedMediaIds(type, query)
        fromTrash -> com.ismartcoding.plain.platform.getTrashedMediaIds(type, query)
        else -> com.ismartcoding.plain.platform.getMediaIds(type, query)
    }
    return JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(
        type,
        com.ismartcoding.plain.features.mediaactions.MediaAction.valueOf(action.uppercase()),
        ids,
        fromTrash,
        destDir,
    ))
}
