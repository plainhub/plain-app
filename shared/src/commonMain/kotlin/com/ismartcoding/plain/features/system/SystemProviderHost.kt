package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.extensions.parseEpochMillis
import com.ismartcoding.plain.features.contact.DContentItem
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.pinyin.Pinyin
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
        else -> error("Unsupported provider operation")
    }
}
