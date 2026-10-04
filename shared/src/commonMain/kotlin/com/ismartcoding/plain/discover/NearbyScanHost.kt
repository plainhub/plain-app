package com.ismartcoding.plain.discover

import com.ismartcoding.plain.platform.bleTransport
import com.ismartcoding.plain.platform.getDeviceIP4sWithPrefixLength
import kotlinx.serialization.json.*

object NearbyScanHost {
    fun facts(): JsonObject = buildJsonObject {
        put("paused", bleTransport().createScanner().isScanPaused())
        put("interfaces", JsonArray(getDeviceIP4sWithPrefixLength().map { (ip, prefix) ->
            buildJsonObject { put("ip", ip); put("prefixLength", prefix.toInt()) }
        }))
    }
}
