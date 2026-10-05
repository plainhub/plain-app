package com.ismartcoding.plain.discover

import com.ismartcoding.plain.platform.bleTransport
import kotlinx.serialization.json.*

object NearbyScanHost {
    fun facts(): JsonObject = buildJsonObject {
        put("paused", bleTransport().createScanner().isScanPaused())
    }
}
