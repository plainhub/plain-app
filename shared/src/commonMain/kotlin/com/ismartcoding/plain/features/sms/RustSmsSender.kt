package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.api.RustContentApi
import kotlinx.serialization.json.*

object RustSmsSender {
    suspend fun send(
        number: String,
        body: String,
        subscriptionId: Int?,
        clientId: String?,
        clientRequestId: String?,
    ) {
        RustContentApi.postJson("system/sms-send", buildJsonObject {
            put("number", number)
            put("body", body)
            put("subscriptionId", subscriptionId?.let(::JsonPrimitive) ?: JsonNull)
            put("clientId", clientId?.let(::JsonPrimitive) ?: JsonNull)
            put("clientRequestId", clientRequestId?.let(::JsonPrimitive) ?: JsonNull)
        })
    }
}
