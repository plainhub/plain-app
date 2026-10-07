package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.enums.ChannelSystemMessageType
import com.ismartcoding.plain.platform.getDeviceType
import kotlinx.serialization.json.*

object RustChannelOutgoingStore {
    suspend fun prepare(channel: DChatChannel, type: ChannelSystemMessageType, target: String = ""): JsonObject = callChatStore("prepareChannel") {
        put("channel", RustChannelStore.encode(channel))
        put("message_type", type.name)
        put("target", target)
        put("name", TempData.deviceName.value)
        put("device_type", getDeviceType().name)
    }.jsonObject
    suspend fun wire(prepared: JsonObject): String = callChatStore("signChannel") {
        put("message_type", prepared.getValue("messageType"))
        put("payload", prepared.getValue("payload"))
    }.jsonPrimitive.content
}

private suspend fun callChatStore(action: String, fields: JsonObjectBuilder.() -> Unit = {}): JsonElement =
    RustContentApi.postJsonOrThrow("chat/store", buildJsonObject { put("action", action); fields() }).getValue("result")
