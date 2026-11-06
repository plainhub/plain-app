package com.ismartcoding.plain.rust.host.provider

import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.network.getNetworkType
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.platform.system.clearMediaCache
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.readBatterySample
import com.ismartcoding.plain.platform.system.readSystemProperties
import com.ismartcoding.plain.rust.host.*
import com.ismartcoding.plain.lib.coMain
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

internal object SystemAppHost {
    internal suspend fun handle(method: String, params: JsonObject): HostResult = when (method) {
        "systemSetClipboard" -> {
            setClipboardText("plain", JsonHelper.jsonDecodeFromElement<ClipboardTextRequest>(params).text)
            hostResult(true)
        }
        "systemAccessFacts" -> {
            val request = JsonHelper.jsonDecodeFromElement<AccessFactsRequest>(params)
            hostResult(AccessFacts(request.features.associate { it.name to it.isGranted() }))
        }
        "systemRequestAccess" -> {
            val feature = JsonHelper.jsonDecodeFromElement<AccessRequest>(params).feature
            MainViewModel.pendingWebAccessRequest.value = com.ismartcoding.plain.models.WebAccessRequest(feature)
            hostResult(true)
        }
        "systemNetworkType" -> hostResult(getNetworkType().name)
        "systemProperties" -> hostResult(readSystemProperties(
            JsonHelper.jsonDecodeFromElement<SystemPropertiesRequest>(params).names,
        ))
        "systemBatterySample" -> hostResult(readBatterySample())
        "systemCapabilities" -> hostResult(getDeviceCapabilities().map { it.name })
        "systemClearMediaCache" -> {
            clearMediaCache()
            hostResult(true)
        }
        "systemRelaunchApp" -> {
            coMain { MainViewModel.relaunchApp() }
            hostResult(true)
        }
        else -> error("Unsupported provider operation")
    }

}

@Serializable
private data class ClipboardTextRequest(val text: String)

@Serializable
private data class AccessRequest(val feature: AccessFeature)

@Serializable
private data class AccessFactsRequest(val features: List<AccessFeature>)

@Serializable
private data class SystemPropertiesRequest(val names: List<String>)
