package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.DPackageInfo
import kotlinx.serialization.json.*

object RustSystemProviders {
    private suspend fun call(action: String, query: String, offset: Int = 0, limit: Int = 0, sortBy: FileSortBy = FileSortBy.NAME_ASC): JsonObject =
        RustContentApi.postJson("system/providers", buildJsonObject {
            put("action", action); put("query", query); put("offset", offset); put("limit", limit); put("sortBy", sortBy.name)
        })
    suspend fun providerWhere(provider: com.ismartcoding.plain.enums.DataType, query: String): com.ismartcoding.plain.helpers.ContentWhere {
        val result = RustContentApi.postJson("system/provider-plan", buildJsonObject { put("provider", provider.name); put("query", query) })
        return com.ismartcoding.plain.helpers.ContentWhere().apply {
            result.getValue("clauses").jsonArray.forEach { add(it.jsonPrimitive.content) }
            args.addAll(result.getValue("args").jsonArray.map { it.jsonPrimitive.content })
        }
    }
    suspend fun deleteRecords(provider: com.ismartcoding.plain.enums.DataType, ids: Collection<String>): Int =
        RustContentApi.postJson("system/provider-delete", buildJsonObject { put("provider", provider.name); put("ids", JsonArray(ids.map(::JsonPrimitive))) }).getValue("count").jsonPrimitive.int
    suspend fun packages(query: String, limit: Int, offset: Int, sortBy: FileSortBy): List<DPackageInfo> =
        JsonHelper.jsonDecode(call("packages", query, offset, limit, sortBy).getValue("items").toString())
    suspend fun packageCount(query: String): Int = call("packageCount", query).getValue("count").jsonPrimitive.int
    suspend fun notifications(query: String, offset: Int, limit: Int): List<DNotification> =
        JsonHelper.jsonDecode(call("notifications", query, offset, limit).getValue("items").toString())
    suspend fun deleteNotifications(ids: List<String>): Int = RustContentApi.postJson("system/notification", buildJsonObject {
        put("action", "delete"); put("ids", JsonArray(ids.map(::JsonPrimitive)))
    }).getValue("count").jsonPrimitive.int
    suspend fun replyNotification(id: String, actionIndex: Int, text: String): Boolean = RustContentApi.postJson("system/notification", buildJsonObject {
        put("action", "reply"); put("id", id); put("actionIndex", actionIndex); put("text", text)
    }).getValue("ok").jsonPrimitive.boolean
    suspend fun notificationCount(query: String): Int = call("notificationCount", query).getValue("count").jsonPrimitive.int
}
