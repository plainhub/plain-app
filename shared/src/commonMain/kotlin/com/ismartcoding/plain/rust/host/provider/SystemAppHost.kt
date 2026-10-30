package com.ismartcoding.plain.rust.host.provider

import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.features.web.WebSettingsFeature
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.network.getNetworkType
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.platform.system.clearMediaCache
import com.ismartcoding.plain.platform.system.BackgroundExecution
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.readBatterySample
import com.ismartcoding.plain.platform.system.readSystemProperties
import com.ismartcoding.plain.platform.system.scheduleBackgroundWake
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
        "systemPermissionFacts" -> run {
            val request = JsonHelper.jsonDecodeFromElement<PermissionRequest>(params)
            val granted = request.permissions.associate { name ->
                name to Permission.valueOf(name).isGranted()
            }
            hostResult(PermissionFacts(
                granted = granted,
            ))
        }
        "systemOpenWebSettings" -> {
            val feature = JsonHelper.jsonDecodeFromElement<WebSettingsRequest>(params).feature
                ?.let { name ->
                    WebSettingsFeature.entries.firstOrNull { it.name == name }
                }
            MainViewModel.pendingWebAccessRequest.value = com.ismartcoding.plain.models.WebAccessRequest(feature)
            hostResult(true)
        }
        "systemNetworkType" -> hostResult(getNetworkType().name)
        "systemScheduleBackgroundWake" -> {
            scheduleBackgroundWake(JsonHelper.jsonDecodeFromElement<BackgroundWakeRequest>(params).atMs)
            hostResult(true)
        }
        "systemCompleteBackgroundExecution" -> {
            val result = JsonHelper.jsonDecodeFromElement<BackgroundExecutionResult>(params)
            BackgroundExecution.complete(result.id, result.success)
            hostResult(true)
        }
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
private data class PermissionRequest(val permissions: List<String>)

@Serializable
private data class WebSettingsRequest(val feature: String? = null)

@Serializable
private data class BackgroundWakeRequest(val atMs: Long? = null)

@Serializable
private data class BackgroundExecutionResult(val id: String, val success: Boolean)

@Serializable
private data class SystemPropertiesRequest(val names: List<String>)
