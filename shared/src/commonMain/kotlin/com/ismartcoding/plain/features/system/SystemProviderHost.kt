package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.enums.WebSettingsFeature
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.data.getGeo
import com.ismartcoding.plain.extensions.parseEpochMillis
import com.ismartcoding.plain.features.contact.DContentItem
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.pinyin.Pinyin
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.installedPackageFacts
import com.ismartcoding.plain.platform.notificationFacts
import com.ismartcoding.plain.platform.isGranted
import kotlinx.serialization.json.*

private fun contactInput(params: JsonObject): com.ismartcoding.plain.httpserver.models.ContactInput =
    JsonHelper.jsonDecode(params.getValue("input").toString())

/**
 * The public contact contract, field for field. The `ContactsContract`
 * `DATA2` code stays a raw int on each detail row: Rust owns the enum, so the
 * wire keeps the number and the mapping stays in one place.
 */
private fun contactFacts(contact: DContact): JsonObject {
    fun contentItem(item: DContentItem): JsonObject = buildJsonObject {
        put("value", JsonPrimitive(item.value))
        put("type", JsonPrimitive(item.type))
        put("label", JsonPrimitive(item.label))
    }
    fun contentItems(items: List<DContentItem>): JsonArray = JsonArray(items.map(::contentItem))
    return buildJsonObject {
        put("id", JsonPrimitive(contact.id))
        put("prefix", JsonPrimitive(contact.prefix))
        put("firstName", JsonPrimitive(contact.givenName))
        put("middleName", JsonPrimitive(contact.middleName))
        put("lastName", JsonPrimitive(contact.familyName))
        put("suffix", JsonPrimitive(contact.suffix))
        put("nickname", JsonPrimitive(contact.nickname))
        put("photoId", JsonPrimitive(contact.photoUri))
        put("phoneNumbers", JsonArray(contact.phoneNumbers.map { phone ->
            buildJsonObject {
                put("value", JsonPrimitive(phone.value))
                put("type", JsonPrimitive(phone.type))
                put("label", JsonPrimitive(phone.label))
                put("normalizedNumber", JsonPrimitive(phone.normalizedNumber))
            }
        }))
        put("emails", contentItems(contact.emails))
        put("addresses", contentItems(contact.addresses))
        put("events", contentItems(contact.events))
        put("websites", contentItems(contact.websites))
        put("ims", contentItems(contact.ims))
        put("source", JsonPrimitive(contact.source))
        put("starred", JsonPrimitive(contact.starred != 0))
        put("contactId", JsonPrimitive(contact.contactId))
        put("thumbnailId", JsonPrimitive(contact.thumbnailUri))
        put("notes", JsonPrimitive(contact.notes))
        put("groups", JsonArray(contact.groups.map { group ->
            buildJsonObject {
                put("id", JsonPrimitive(group.id.toString()))
                put("name", JsonPrimitive(group.name))
            }
        }))
        put("organization", contact.organization?.let { organization ->
            buildJsonObject {
                put("company", JsonPrimitive(organization.company))
                put("title", JsonPrimitive(organization.title))
            }
        } ?: JsonNull)
        put("ringtone", JsonPrimitive(contact.ringtone))
        put("updatedAt", JsonPrimitive(contact.updatedAt.toString()))
    }
}

/** The public call-log contract; the geo lookup stays on the platform. */
private fun callFacts(call: DCall): JsonObject {
    val geo = call.getGeo()
    return buildJsonObject {
        put("id", JsonPrimitive(call.id))
        put("number", JsonPrimitive(call.number))
        put("name", JsonPrimitive(call.name))
        put("photoId", JsonPrimitive(com.ismartcoding.plain.helpers.getFileId(call.photoUri)))
        put("startedAt", JsonPrimitive(call.startedAt.toString()))
        put("durationSec", JsonPrimitive(call.durationSec))
        put("type", JsonPrimitive(call.type))
        put("accountId", JsonPrimitive(call.accountId))
        put("geo", geo?.let { value ->
            buildJsonObject {
                put("country", JsonPrimitive(value.country))
                put("numberType", JsonPrimitive(value.numberType))
                put("carrier", JsonPrimitive(value.carrier))
                put("description", JsonPrimitive(value.description))
            }
        } ?: JsonNull)
    }
}

object SystemProviderHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemPackageFacts" -> JsonArray(installedPackageFacts().map { item ->
            buildJsonObject {
                put("item", Json.parseToJsonElement(JsonHelper.jsonEncode(item)))
                put("nameSortKey", Pinyin.toPinyin(item.name).lowercase())
            }
        })
        "systemPackageStatuses" -> {
            val ids = params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }
            JsonObject(
                com.ismartcoding.plain.platform.getPackageInfoMap(ids).mapValues { (_, pkg) ->
                    if (pkg == null) JsonNull else Json.parseToJsonElement(JsonHelper.jsonEncode(pkg))
                }
            )
        }
        "systemInstallPackage" -> {
            val path = params.getValue("path").jsonPrimitive.content
            val result = try {
                com.ismartcoding.plain.platform.installPackage(path)
            } catch (e: Exception) {
                throw IllegalStateException("Installation failed: ${e.message}", e)
            }
            buildJsonObject {
                put("packageName", JsonPrimitive(result.packageName))
                put("lastUpdateTime", result.lastUpdateTime?.let { JsonPrimitive(it.toString()) } ?: JsonNull)
                put("isNew", JsonPrimitive(result.isNew))
            }
        }
        "systemUninstallPackages" -> {
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.forEach {
                com.ismartcoding.plain.platform.uninstallPackage(it)
            }
            JsonPrimitive(true)
        }
        "systemSetClipboard" -> {
            com.ismartcoding.plain.platform.setClipboardText("plain", params.getValue("text").jsonPrimitive.content)
            JsonPrimitive(true)
        }
        "systemNotificationFacts" -> Json.parseToJsonElement(JsonHelper.jsonEncode(notificationFacts()))
        "systemMmsTextFacts", "systemSmsCountFacts", "systemSmsRowsFacts", "systemSmsIdsFacts", "systemSmsConversationFacts", "systemSmsThreadFacts" -> com.ismartcoding.plain.platform.systemSmsFacts(method, params)
        "systemEpochMillis" -> buildJsonObject {
            params.getValue("values").jsonArray.forEach { value ->
                val text = value.jsonPrimitive.content
                put(text, text.parseEpochMillis()?.let(::JsonPrimitive) ?: JsonNull)
            }
        }
        "systemPermissionFacts" -> buildJsonObject {
            val granted = buildJsonObject {
                params.getValue("permissions").jsonArray.forEach { item ->
                    val name = item.jsonPrimitive.content
                    put(name, com.ismartcoding.plain.platform.Permission.valueOf(name).isGranted())
                }
            }
            put("granted", granted)
        }
        "systemSendSms" -> {
            com.ismartcoding.plain.platform.sendSmsText(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("body").jsonPrimitive.content,
                params["subscriptionId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.int,
                params["clientId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                params["clientRequestId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
            )
            JsonPrimitive(true)
        }
        "zipItemsFacts" -> {
            val type = params.getValue("type").jsonPrimitive.content
            val items = com.ismartcoding.plain.platform.searchZipItems(
                type,
                params.getValue("query").jsonPrimitive.content,
                params.getValue("id").jsonPrimitive.content,
            ).filter { com.ismartcoding.plain.platform.fileExists(it.sourcePath) }
            buildJsonObject {
                put("items", JsonArray(items.map { entry ->
                    buildJsonObject {
                        put("path", JsonPrimitive(entry.sourcePath))
                        put("name", JsonPrimitive(entry.entryName))
                    }
                }))
            }
        }
        "uploadTmpDirFacts" -> buildJsonObject {
            put("path", com.ismartcoding.plain.platform.getUploadTmpDirPath())
        }
        "scanFilesFacts" -> {
            com.ismartcoding.plain.platform.scanFiles(
                params.getValue("paths").jsonArray.map { it.jsonPrimitive.content }.toTypedArray()
            )
            JsonPrimitive(true)
        }
        "systemContactFacts" -> {
            val query = params.getValue("query").jsonPrimitive.content
            val offset = params.getValue("offset").jsonPrimitive.int
            val limit = params.getValue("limit").jsonPrimitive.int
            JsonArray(
                com.ismartcoding.plain.platform.searchMedia(
                    com.ismartcoding.plain.enums.DataType.CONTACT, query, limit, offset,
                    com.ismartcoding.plain.features.file.FileSortBy.DATE_DESC,
                ).filterIsInstance<DContact>().map(::contactFacts)
            )
        }
        "systemContactCount" -> JsonPrimitive(
            com.ismartcoding.plain.platform.countMedia(
                com.ismartcoding.plain.enums.DataType.CONTACT,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemContactIds" -> JsonArray(
            com.ismartcoding.plain.platform.getMediaIds(
                com.ismartcoding.plain.enums.DataType.CONTACT,
                params.getValue("query").jsonPrimitive.content,
            ).map(::JsonPrimitive)
        )
        "systemContactGroupFacts" -> JsonArray(
            com.ismartcoding.plain.platform.getContactGroups().map { group ->
                buildJsonObject {
                    put("id", JsonPrimitive(group.id.toString()))
                    put("name", JsonPrimitive(group.name))
                }
            }
        )
        "systemContactSources" -> JsonArray(
            com.ismartcoding.plain.platform.getContactSources().map { source ->
                buildJsonObject {
                    put("name", JsonPrimitive(source.name))
                    put("type", JsonPrimitive(source.type))
                }
            }
        )
        "systemCallFacts" -> {
            val query = params.getValue("query").jsonPrimitive.content
            val offset = params.getValue("offset").jsonPrimitive.int
            val limit = params.getValue("limit").jsonPrimitive.int
            JsonArray(
                com.ismartcoding.plain.platform.searchMedia(
                    com.ismartcoding.plain.enums.DataType.CALL, query, limit, offset,
                    com.ismartcoding.plain.features.file.FileSortBy.DATE_DESC,
                ).filterIsInstance<DCall>().map(::callFacts)
            )
        }
        "systemCallCount" -> JsonPrimitive(
            com.ismartcoding.plain.platform.countMedia(
                com.ismartcoding.plain.enums.DataType.CALL,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemCallIds" -> JsonArray(
            com.ismartcoding.plain.platform.getMediaIds(
                com.ismartcoding.plain.enums.DataType.CALL,
                params.getValue("query").jsonPrimitive.content,
            ).map(::JsonPrimitive)
        )
        "systemMakeCall" -> {
            com.ismartcoding.plain.platform.call(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("showDialer").jsonPrimitive.boolean,
            )
            JsonPrimitive(true)
        }
        "systemTrashSms" -> JsonPrimitive(
            com.ismartcoding.plain.platform.trashSms(params.getValue("query").jsonPrimitive.content)
        )
        "systemRestoreSms" -> JsonPrimitive(
            com.ismartcoding.plain.platform.restoreSms(params.getValue("query").jsonPrimitive.content)
        )
        "systemDeleteSms" -> JsonPrimitive(
            com.ismartcoding.plain.platform.deleteSms(params.getValue("query").jsonPrimitive.content)
        )
        "systemSendMms" -> JsonPrimitive(
            com.ismartcoding.plain.httpserver.mainschemas.sendMms(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("body").jsonPrimitive.content,
                params.getValue("attachmentPaths").jsonArray.map { it.jsonPrimitive.content },
                com.ismartcoding.plain.httpserver.models.ID(params.getValue("threadId").jsonPrimitive.content),
            )
        )
        "systemScreenMirrorState" -> {
            val codec = com.ismartcoding.plain.platform.getScreenMirrorVideoCodec()
            buildJsonObject {
                put("running", JsonPrimitive(com.ismartcoding.plain.platform.isScreenMirrorRunning()))
                put("controlEnabled", JsonPrimitive(com.ismartcoding.plain.platform.isScreenMirrorControlEnabled()))
                put("codec", codec?.let { value ->
                    buildJsonObject {
                        put("annexB", JsonPrimitive(value.annexB))
                        put("keyFrame", value.keyFrame?.let { JsonPrimitive(it) } ?: JsonNull)
                    }
                } ?: JsonNull)
            }
        }
        "systemScreenMirrorQuality" -> Json.parseToJsonElement(
            JsonHelper.jsonEncode(UserPrefs.screenMirrorQualityValue().toModel())
        )
        "systemStartScreenMirror" -> {
            com.ismartcoding.plain.platform.applyScreenMirrorQualityPreference()
            sendEvent(
                com.ismartcoding.plain.events.HStartScreenMirrorEvent(
                    params.getValue("audio").jsonPrimitive.boolean
                )
            )
            JsonPrimitive(true)
        }
        "systemStopScreenMirror" -> {
            com.ismartcoding.plain.platform.stopScreenMirror()
            JsonPrimitive(true)
        }
        "systemRequestScreenMirrorAudio" -> {
            val granted = com.ismartcoding.plain.platform.Permission.RECORD_AUDIO.isGranted()
            if (!granted) {
                sendEvent(
                    com.ismartcoding.plain.events.HRequestScreenMirrorAudioEvent()
                )
            }
            JsonPrimitive(granted)
        }
        "systemRequestScreenMirrorKeyFrame" -> {
            com.ismartcoding.plain.platform.requestScreenMirrorKeyFrame()
            JsonPrimitive(true)
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
            JsonPrimitive(true)
        }
        "systemOpenAccessibilitySettings" -> {
            sendEvent(com.ismartcoding.plain.events.HOpenAccessibilitySettingsEvent())
            JsonPrimitive(true)
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
            JsonPrimitive(true)
        }
        "systemImageSearchStatus" -> Json.parseToJsonElement(
            JsonHelper.jsonEncode(com.ismartcoding.plain.platform.buildImageSearchStatus())
        )
        "systemEnableImageSearch" -> {
            sendEvent(com.ismartcoding.plain.events.HEnableImageSearchEvent())
            JsonPrimitive(true)
        }
        "systemDisableImageSearch" -> {
            sendEvent(com.ismartcoding.plain.events.HDisableImageSearchEvent())
            JsonPrimitive(true)
        }
        "systemCancelImageModelDownload" -> {
            sendEvent(com.ismartcoding.plain.events.HCancelImageModelDownloadEvent())
            JsonPrimitive(true)
        }
        "systemStartImageIndex" -> {
            com.ismartcoding.plain.platform.startImageIndexFullScan(
                params.getValue("force").jsonPrimitive.boolean
            )
            JsonPrimitive(true)
        }
        "systemCancelImageIndex" -> {
            com.ismartcoding.plain.platform.cancelImageIndex()
            JsonPrimitive(true)
        }
        "systemDeleteRecords" -> JsonArray(com.ismartcoding.plain.platform.deleteSystemProviderFacts(
            com.ismartcoding.plain.enums.DataType.valueOf(params.getValue("provider").jsonPrimitive.content),
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()).map(::JsonPrimitive))
        "systemCreateContact" -> JsonPrimitive(com.ismartcoding.plain.platform.createContact(
            contactInput(params)))
        "systemUpdateContact" -> {
            com.ismartcoding.plain.platform.updateContact(
                params.getValue("id").jsonPrimitive.content,
                contactInput(params),
            )
            JsonPrimitive(true)
        }
        "systemCreateContactGroup" -> JsonPrimitive(com.ismartcoding.plain.platform.createContactGroup(
            params.getValue("name").jsonPrimitive.content,
            params.getValue("accountName").jsonPrimitive.content,
            params.getValue("accountType").jsonPrimitive.content,
        ).id.toString())
        "systemUpdateContactGroup" -> {
            com.ismartcoding.plain.platform.updateContactGroup(
                params.getValue("id").jsonPrimitive.content,
                params.getValue("name").jsonPrimitive.content,
            )
            JsonPrimitive(true)
        }
        "systemDeleteContactGroup" -> {
            com.ismartcoding.plain.platform.deleteContactGroup(params.getValue("id").jsonPrimitive.content)
            JsonPrimitive(true)
        }
        "systemCancelNotifications" -> JsonArray(com.ismartcoding.plain.platform.cancelNotificationFacts(
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()).map(::JsonPrimitive))
        "systemReplyNotification" -> JsonPrimitive(com.ismartcoding.plain.platform.replyNotification(
            params.getValue("id").jsonPrimitive.content, params.getValue("actionIndex").jsonPrimitive.int, params.getValue("text").jsonPrimitive.content))
        "fileMetadataFacts" -> {
            val path = params.getValue("path").jsonPrimitive.content
            buildJsonObject {
                put("size", com.ismartcoding.plain.platform.statFile(path)?.size ?: 0)
                put("mimeType", com.ismartcoding.plain.platform.getContentTypeForPath(path).orEmpty())
            }
        }
        "systemMountFacts" -> JsonArray(com.ismartcoding.plain.httpserver.loaders.MountsLoader.load().map { mount ->
            buildJsonObject {
                put("id", JsonPrimitive(mount.id.value)); put("name", JsonPrimitive(mount.name))
                put("path", JsonPrimitive(mount.path)); put("mountPoint", JsonPrimitive(mount.mountPoint))
                put("fsType", JsonPrimitive(mount.fsType)); put("totalBytes", JsonPrimitive(mount.totalBytes))
                put("usedBytes", JsonPrimitive(mount.usedBytes)); put("freeBytes", JsonPrimitive(mount.freeBytes))
                put("remote", JsonPrimitive(mount.remote)); put("alias", JsonPrimitive(mount.alias))
                put("driveType", JsonPrimitive(mount.driveType.name)); put("diskId", JsonPrimitive(mount.diskId))
            }
        })
        "systemRecentFileFacts" -> JsonArray(com.ismartcoding.plain.platform.getRecentFiles().map(::fileFacts))
        "systemImageFileInfo" -> Json.parseToJsonElement(
            JsonHelper.jsonEncode(fileInfoFacts(method, params)))
        "systemVideoFileInfo" -> Json.parseToJsonElement(
            JsonHelper.jsonEncode(fileInfoFacts(method, params)))
        "systemAudioFileInfo" -> Json.parseToJsonElement(
            JsonHelper.jsonEncode(fileInfoFacts(method, params)))
        "systemMediaBucketItemFacts" -> JsonArray(
            com.ismartcoding.plain.platform.mediaBucketItemFacts(mediaDataType(params)).map { item ->
                buildJsonObject {
                    put("id", JsonPrimitive(item.id)); put("name", JsonPrimitive(item.name))
                    put("size", JsonPrimitive(item.size)); put("path", JsonPrimitive(item.path))
                    put("sortName", JsonPrimitive(item.sortName))
                }
            })
        "systemMediaRows" -> {
            val items = com.ismartcoding.plain.platform.searchMedia(
                mediaDataType(params),
                params.getValue("query").jsonPrimitive.content,
                params.getValue("limit").jsonPrimitive.int,
                params.getValue("offset").jsonPrimitive.int,
                fileSortBy(params),
            )
            JsonArray(items.map { mediaFacts(it) })
        }
        "systemImageRows" -> {
            val items = com.ismartcoding.plain.platform.searchImagesCombined(
                params.getValue("queryText").jsonPrimitive.content,
                params.getValue("extraQuery").jsonPrimitive.content,
                params.getValue("limit").jsonPrimitive.int,
                params.getValue("offset").jsonPrimitive.int,
                fileSortBy(params),
            )
            JsonArray(items.map { mediaFacts(it) })
        }
        "systemMediaCount" -> JsonPrimitive(com.ismartcoding.plain.platform.countMedia(
            mediaDataType(params), params.getValue("query").jsonPrimitive.content))
        "systemImageCount" -> JsonPrimitive(com.ismartcoding.plain.platform.countImagesCombined(
            params.getValue("queryText").jsonPrimitive.content,
            params.getValue("extraQuery").jsonPrimitive.content))
        "systemDocExtGroups" -> JsonArray(
            com.ismartcoding.plain.platform.getDocExtGroups("").map { (ext, count) ->
                buildJsonObject { put("ext", JsonPrimitive(ext)); put("count", JsonPrimitive(count)) } })
        "systemMediaTagFacts" -> mediaTagFacts(params)
        "systemTagQueryStubs" -> JsonArray(
            com.ismartcoding.plain.platform.getMediaTagRelationStubs(
                mediaDataType(params), params.getValue("query").jsonPrimitive.content).map { stub ->
                buildJsonObject {
                    put("key", JsonPrimitive(stub.key)); put("title", JsonPrimitive(stub.title))
                    put("size", JsonPrimitive(stub.size))
                }
            })
        "systemTagQueryKeys" -> buildJsonObject {
            put("ids", JsonArray(com.ismartcoding.plain.platform.getMediaIds(
                mediaDataType(params), params.getValue("query").jsonPrimitive.content).map(::JsonPrimitive)))
        }
        // Pref writes go through here rather than straight to the store:
        // `Prefs` keeps an in-memory copy and fans the change out to the
        // live flows the UI collects, so a write that skipped it would
        // persist the value and leave every observer on the old one.
        "systemSetUserPref" -> {
            com.ismartcoding.plain.preferences.Prefs.setUserPref(
                params.getValue("key").jsonPrimitive.content, params.getValue("value"))
            JsonPrimitive(true)
        }
        "systemRemoveUserPref" -> {
            com.ismartcoding.plain.preferences.Prefs.removeUserPref(params.getValue("key").jsonPrimitive.content)
            JsonPrimitive(true)
        }
        "systemDbFacts" -> buildJsonObject {
            put("path", JsonPrimitive(com.ismartcoding.plain.platform.getDbPath()))
            put("tables", JsonArray(com.ismartcoding.plain.platform.getDbTableNames().map(::JsonPrimitive)))
        }
        "systemDbRowCount" -> JsonPrimitive(com.ismartcoding.plain.platform.getDbTableRowCount(
            params.getValue("table").jsonPrimitive.content))
        "systemDbRows" -> buildJsonObject {
            put("rows", JsonArray(com.ismartcoding.plain.platform.getDbTableRows(
                params.getValue("table").jsonPrimitive.content,
                params.getValue("offset").jsonPrimitive.int,
                params.getValue("limit").jsonPrimitive.int).map(::JsonPrimitive)))
        }
        "systemDbColumns" -> buildJsonObject {
            put("columns", Json.parseToJsonElement(JsonHelper.jsonEncode(
                com.ismartcoding.plain.platform.getDbTableColumns(params.getValue("table").jsonPrimitive.content))))
        }
        "systemDbInfo" -> buildJsonObject {
            put("idKey", JsonPrimitive(
                com.ismartcoding.plain.platform.getDbTableInfo(params.getValue("table").jsonPrimitive.content).idKey))
        }
        "systemCreateDbRow" -> JsonPrimitive(com.ismartcoding.plain.platform.createDbTableRow(
            params.getValue("table").jsonPrimitive.content,
            params.getValue("row").jsonPrimitive.content))
        "systemDeleteDbRows" -> JsonPrimitive(com.ismartcoding.plain.platform.deleteDbTableRows(
            params.getValue("table").jsonPrimitive.content,
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }))
        else -> error("Unsupported provider operation")
    }
}

/** The `File` contract row. `mediaId` stays an empty string for non-media
 * entries; Rust maps that to `null` rather than carrying the sentinel. */
private fun fileFacts(file: com.ismartcoding.plain.features.file.DFile): JsonObject = buildJsonObject {
    put("name", JsonPrimitive(file.name)); put("path", JsonPrimitive(file.path))
    put("mediaId", JsonPrimitive(file.mediaId))
    put("createdAt", file.createdAt?.toEpochMilliseconds()?.let(::JsonPrimitive) ?: JsonNull)
    put("updatedAt", file.updatedAt.toEpochMilliseconds())
    put("size", JsonPrimitive(file.size)); put("isDir", JsonPrimitive(file.isDir))
    put("childCount", JsonPrimitive(file.childCount))
}

private fun locationFacts(location: com.ismartcoding.plain.httpserver.models.Location?): JsonElement =
    location?.let {
        buildJsonObject {
            put("latitude", JsonPrimitive(it.latitude)); put("longitude", JsonPrimitive(it.longitude))
        }
    } ?: JsonNull

private fun fileInfoFacts(method: String, params: JsonObject): JsonObject {
    val path = params.getValue("path").jsonPrimitive.content
    return when (method) {
        "systemImageFileInfo" -> com.ismartcoding.plain.platform.loadImageInfo(path).let {
            buildJsonObject {
                put("width", JsonPrimitive(it.width)); put("height", JsonPrimitive(it.height))
                put("location", locationFacts(it.location))
            }
        }
        "systemVideoFileInfo" -> com.ismartcoding.plain.platform.loadVideoInfo(path).let {
            buildJsonObject {
                put("width", JsonPrimitive(it.width)); put("height", JsonPrimitive(it.height))
                put("durationMs", JsonPrimitive(it.durationMs)); put("location", locationFacts(it.location))
            }
        }
        else -> com.ismartcoding.plain.platform.loadAudioInfo(path).let {
            buildJsonObject {
                put("durationMs", JsonPrimitive(it.durationMs)); put("location", locationFacts(it.location))
            }
        }
    }
}

private fun mediaDataType(params: JsonObject) =
    com.ismartcoding.plain.enums.DataType.valueOf(params.getValue("dataType").jsonPrimitive.content)

private fun fileSortBy(params: JsonObject) =
    com.ismartcoding.plain.features.file.FileSortBy.valueOf(params.getValue("sortBy").jsonPrimitive.content)

/** One row of a library list. The three kinds share the `MediaItem`
 * fields; the per-kind extras ride along so a single decoder reads all
 * three lists, with a field a kind does not have reported as null rather
 * than omitted — an omitted field and a null one mean the same thing to
 * the client, but only one of them survives a round trip through a
 * positional row. */
private fun mediaFacts(item: com.ismartcoding.plain.db.IData): JsonObject = when (item) {
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
): JsonObject = buildJsonObject {
    put("id", JsonPrimitive(id)); put("title", JsonPrimitive(title))
    put("path", JsonPrimitive(path)); put("size", JsonPrimitive(size))
    put("bucketId", JsonPrimitive(bucketId)); put("createdAt", JsonPrimitive(createdAt.toString()))
    put("updatedAt", JsonPrimitive(updatedAt.toString()))
    put("durationMs", JsonPrimitive(durationMs))
    put("takenAt", takenAt?.let { JsonPrimitive(it.toString()) } ?: JsonNull)
    put("isFavorite", JsonPrimitive(isFavorite))
}

/** One entry per key that actually has relations; keys with none are
 * omitted and Rust defaults them to an empty list, so an explicit empty
 * row would just be noise on the wire. */
private suspend fun mediaTagFacts(params: JsonObject): JsonArray {
    val type = mediaDataType(params)
    val keys = params.getValue("keys").jsonArray.map { it.jsonPrimitive.content }.toSet()
    if (keys.isEmpty()) return JsonArray(emptyList())
    val relations = com.ismartcoding.plain.features.TagHelper
        .getTagRelationsByKeys(keys, type).groupBy { it.key }
    val tags = com.ismartcoding.plain.features.TagHelper.getAll(type).associateBy { it.id }
    return JsonArray(keys.mapNotNull { key ->
        val ids = relations[key]?.map { it.tagId } ?: return@mapNotNull null
        if (ids.isEmpty()) return@mapNotNull null
        buildJsonObject {
            put("key", JsonPrimitive(key))
            put("tags", JsonArray(ids.mapNotNull { tags[it] }.map { tag ->
                buildJsonObject {
                    put("id", JsonPrimitive(tag.id)); put("name", JsonPrimitive(tag.name))
                    put("count", JsonPrimitive(tag.count))
                }
            }))
        }
    })
}
