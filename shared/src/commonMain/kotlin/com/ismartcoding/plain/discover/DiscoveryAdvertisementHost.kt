package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.*
import kotlinx.serialization.json.*

object DiscoveryAdvertisementHost {
    fun facts(): JsonObject = buildJsonObject {
        put("name", getDeviceName())
        put("deviceType", getDeviceType().name)
        put("version", getAppVersion())
        put("platform", getPlatformName())
        put("ips", JsonArray(getDeviceIP4s().map(::JsonPrimitive)))
        put("awareSupported", isWifiAwareSupported)
        put("awareRunning", TempData.awareRunning.value)
    }
}
