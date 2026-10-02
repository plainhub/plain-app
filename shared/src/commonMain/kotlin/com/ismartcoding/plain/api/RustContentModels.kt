package com.ismartcoding.plain.api

import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.preferences.SystemPrefs
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.*
import kotlin.time.Instant

internal fun gql(value: String): String = JsonPrimitive(value).toString()
internal fun gqlIds(values: Collection<String>): String = values.joinToString(",", "[", "]") { gql(it) }
internal fun selectionQuery(ids: Set<String>): String = "ids:${ids.joinToString(",") }"
internal const val NOTE_FIELDS = "id title content deletedAt createdAt updatedAt"
internal const val FEED_FIELDS = "id name url logo fetchContent lastSyncAt lastError { code detail } createdAt updatedAt"
internal const val ENTRY_FIELDS = "id title url image description author content feedId rawId publishedAt read createdAt updatedAt"
internal const val TAG_FIELDS = "id name type count"
internal fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content
internal fun JsonObject.instant(name: String): Instant = Instant.parse(string(name))
internal fun JsonObject.nullableInstant(name: String): Instant? = this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.let(Instant::parse)
internal fun JsonElement.note(): DNote = jsonObject.let { DNote(id=it.string("id"),title=it.string("title"),content=it.string("content"),deletedAt=it.nullableInstant("deletedAt"),createdAt=it.instant("createdAt"),updatedAt=it.instant("updatedAt")) }
internal fun JsonElement.feed(): DFeed = jsonObject.let { row ->
    val error = row.getValue("lastError").jsonObject
    DFeed(id=row.string("id"),name=row.string("name"),url=row.string("url"),logo=decodeImage(row.string("logo")),fetchContent=row.getValue("fetchContent").jsonPrimitive.boolean,lastSyncAt=row.nullableInstant("lastSyncAt"),lastError=DFeedError(error.string("code"),error.string("detail")),createdAt=row.instant("createdAt"),updatedAt=row.instant("updatedAt"))
}
internal fun JsonElement.entry(): DFeedEntry = jsonObject.let { DFeedEntry(id=it.string("id"),title=it.string("title"),url=it.string("url"),image=decodeImage(it.string("image")),description=it.string("description"),author=it.string("author"),content=it.string("content"),feedId=it.string("feedId"),rawId=it.string("rawId"),publishedAt=it.instant("publishedAt"),read=it.getValue("read").jsonPrimitive.boolean,createdAt=it.instant("createdAt"),updatedAt=it.instant("updatedAt")) }
internal fun JsonElement.tag(): DTag = jsonObject.let { DTag(id=it.string("id"),name=it.string("name"),type=it.getValue("type").jsonPrimitive.int,count=it.getValue("count").jsonPrimitive.int) }

private fun decodeImage(value: String): String {
    if (value.isEmpty() || value.startsWith("http://") || value.startsWith("https://")) return value
    return chaCha20Decrypt(Base64.decode(SystemPrefs.urlToken.value), Base64.decode(value))?.decodeToString() ?: error("Invalid feed image identifier")
}
