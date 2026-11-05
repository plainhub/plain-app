package com.ismartcoding.plain.rust.host.network

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.network.bleTransport
import com.ismartcoding.plain.rust.host.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

object PeerTransportPrewarmHost {
    internal suspend fun scan(params: JsonObject): HostResult {
        val request = JsonHelper.jsonDecodeFromElement<PrewarmScanRequest>(params)
        val device = withTimeoutOrNull(request.timeoutMs) {
            bleTransport().createScanner().scan(request.clientId).firstOrNull {
                it.id == request.clientId
            }
        }
        return hostResult(device?.id)
    }
}

@Serializable
private data class PrewarmScanRequest(val clientId: String, val timeoutMs: Long)
