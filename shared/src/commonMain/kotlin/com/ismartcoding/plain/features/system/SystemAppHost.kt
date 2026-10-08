package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.enums.WebSettingsFeature
import com.ismartcoding.plain.extensions.parseEpochMillis
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.isGranted
import kotlinx.serialization.json.*

internal object SystemAppHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemSetClipboard" -> {
            com.ismartcoding.plain.platform.setClipboardText("plain", params.getValue("text").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemEpochMillis" -> JsonHelper.jsonEncodeToElement(params.getValue("values").jsonArray.associate { value ->
            val text = value.jsonPrimitive.content
            text to text.parseEpochMillis()
        })
        "systemPermissionFacts" -> run {
            val granted = params.getValue("permissions").jsonArray.associate { item ->
                val name = item.jsonPrimitive.content
                name to com.ismartcoding.plain.platform.Permission.valueOf(name).isGranted()
            }
            JsonHelper.jsonEncodeToElement(PermissionFacts(
                granted = granted,
            ))
        }
        "systemOpenAccessibilitySettings" -> {
            sendEvent(com.ismartcoding.plain.events.HOpenAccessibilitySettingsEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemOpenWebSettings" -> {
            val feature = params["feature"]
                ?.takeUnless { it is JsonNull }
                ?.jsonPrimitive
                ?.content
                ?.let { name ->
                    WebSettingsFeature.entries.firstOrNull { it.name == name }
                }
            sendEvent(com.ismartcoding.plain.events.HOpenWebSettingsEvent(feature))
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemDeviceInfoFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getDeviceInfo())
        "systemDeviceStatusFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getDeviceStatus())
        "systemAppFacts" -> appFacts()
        "systemAppLogFacts" -> appLogFacts(params)
        "systemClearAppLogs" -> {
            com.ismartcoding.plain.platform.clearLatestLogFile()
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemSetTempValue" -> {
            com.ismartcoding.plain.helpers.TempHelper.setValue(
                params.getValue("key").jsonPrimitive.content, params.getValue("value").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemRelaunchApp" -> {
            com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.RestartAppEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemUpdateDeviceName" -> {
            val name = params.getValue("name").jsonPrimitive.content
            com.ismartcoding.plain.preferences.UserPrefs.deviceName.value = name
            com.ismartcoding.plain.TempData.deviceName.value = name
            com.ismartcoding.plain.discover.MdnsDiscoverManager.updateAdvertisedService()
            JsonHelper.jsonEncodeToElement(true)
        }
        else -> error("Unsupported provider operation")
    }

    /** Log lines, newest first. A non-blank `text` filters the whole buffer
     * before paging, so a search cannot be cut short by the page size; Rust
     * passes `pathOnly` when it wants the log file path instead. */
    private suspend fun appLogFacts(params: JsonObject): JsonElement {
        if (params["pathOnly"]?.jsonPrimitive?.boolean == true) {
            return JsonHelper.jsonEncodeToElement(AppLogFacts(
                path = com.ismartcoding.plain.platform.getLatestLogFilePath(),
                lines = emptyList(),
            ))
        }
        val offset = params.getValue("offset").jsonPrimitive.int
        val limit = params.getValue("limit").jsonPrimitive.int
        val text = params.getValue("query").jsonPrimitive.content.trim()
        val lines = if (text.isEmpty()) {
            com.ismartcoding.plain.platform.readLogLinesNewestFirst(offset, limit)
        } else {
            com.ismartcoding.plain.platform.readLogLinesNewestFirst(0, Int.MAX_VALUE)
                .filter { it.contains(text, ignoreCase = true) }
                .drop(offset.coerceAtLeast(0))
                .take(limit.coerceAtLeast(0))
        }
        return JsonHelper.jsonEncodeToElement(AppLogFacts(
            path = com.ismartcoding.plain.platform.getLatestLogFilePath(),
            lines = lines,
        ))
    }

    /** The `App` contract row. Capabilities and permissions are named rather
     * than encoded: both enums are the contract's own, so there is no mapping. */
    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private suspend fun appFacts(): JsonElement = JsonHelper.jsonEncodeToElement(AppFacts(
        clientId = com.ismartcoding.plain.TempData.clientId,
        urlToken = kotlin.io.encoding.Base64.encode(com.ismartcoding.plain.TempData.urlToken),
        httpPort = UserPrefs.httpPort.value,
        httpsPort = UserPrefs.httpsPort.value,
        appDir = com.ismartcoding.plain.platform.appDir(),
        deviceName = com.ismartcoding.plain.TempData.deviceName.value,
        deviceType = com.ismartcoding.plain.platform.getDeviceType().name,
        capabilities = com.ismartcoding.plain.platform.getDeviceCapabilities().map { it.name },
        buildChannel = com.ismartcoding.plain.enums.AppChannelType
            .fromString(com.ismartcoding.plain.buildChannel).name,
        permissions = com.ismartcoding.plain.features.getGrantedWebPermissionsAsync().map { it.name },
        downloadsDir = com.ismartcoding.plain.platform.getDownloadsDirPath(),
        developerMode = UserPrefs.developerMode.value,
        debug = com.ismartcoding.plain.platform.isDebugBuild(),
    ))
}
