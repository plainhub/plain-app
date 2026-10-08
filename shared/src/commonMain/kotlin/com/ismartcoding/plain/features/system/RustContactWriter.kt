package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.features.contact.ContactInput
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

/**
 * Contact and contact-group writes go through Rust, which enforces the
 * `WRITE_CONTACTS` API permission and validates the payload before asking the
 * platform to write. Only the ContactsContract calls stay on the host.
 */
object RustContactWriter {
    private suspend fun call(action: String, params: JsonObject): JsonObject {
        val request = JsonObject(params + ("action" to JsonPrimitive(action)))
        return RustContentApi.postJsonOrThrow("system/contact-write", request)
    }

    suspend fun create(input: ContactInput): String =
        call("createContact", buildJsonObject { put("input", Json.parseToJsonElement(JsonHelper.jsonEncode(input))) })
            .getValue("id").jsonPrimitive.content

    suspend fun update(id: String, input: ContactInput) {
        call("updateContact", buildJsonObject {
            put("id", JsonPrimitive(id))
            put("input", Json.parseToJsonElement(JsonHelper.jsonEncode(input)))
        })
    }

    suspend fun createGroup(name: String, accountName: String, accountType: String): String =
        call("createGroup", buildJsonObject {
            put("name", JsonPrimitive(name))
            put("accountName", JsonPrimitive(accountName))
            put("accountType", JsonPrimitive(accountType))
        }).getValue("id").jsonPrimitive.content

    suspend fun updateGroup(id: String, name: String) {
        call("updateGroup", buildJsonObject {
            put("id", JsonPrimitive(id))
            put("name", JsonPrimitive(name))
        })
    }

    suspend fun deleteGroup(id: String) {
        call("deleteGroup", buildJsonObject { put("id", JsonPrimitive(id)) })
    }
}
