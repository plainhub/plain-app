package com.ismartcoding.plain.chat

import com.ismartcoding.plain.api.RustContentApi
import kotlinx.serialization.json.*

internal suspend fun callChatStore(action: String, fields: JsonObjectBuilder.() -> Unit = {}): JsonElement =
    RustContentApi.postJsonOrThrow("chat/store", buildJsonObject { put("action", action); fields() }, longRunning = true).getValue("result")
