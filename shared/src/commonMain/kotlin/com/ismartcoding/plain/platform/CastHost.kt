package com.ismartcoding.plain.platform

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object CastHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemCastAddressFacts" -> JsonHelper.jsonEncodeToElement(CastAddressFacts(
            UrlHelper.buildUrl("http", getDeviceIP4(), UserPrefs.httpPort.value),
            TempData.deviceName.value.ifEmpty { getDeviceName() },
        ))
        else -> error("Unsupported cast platform operation")
    }
}
