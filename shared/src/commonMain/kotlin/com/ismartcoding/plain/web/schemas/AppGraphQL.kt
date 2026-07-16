package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.preferences.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.events.HOpenAccessibilitySettingsEvent
import com.ismartcoding.plain.events.HOpenWebSettingsEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.appDir
import com.ismartcoding.plain.platform.getBattery
import com.ismartcoding.plain.platform.getDeviceInfo
import com.ismartcoding.plain.platform.getDownloadsDirPath
import com.ismartcoding.plain.platform.getInternalStoragePath
import com.ismartcoding.plain.platform.getSDCardPath
import com.ismartcoding.plain.platform.getUsbDiskPaths
import com.ismartcoding.plain.platform.isUsbConnected
import com.ismartcoding.plain.platform.isDebugBuild
import com.ismartcoding.plain.platform.getAppVersionCode
import com.ismartcoding.plain.platform.getSdkInt
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.helpers.TempHelper
import com.ismartcoding.plain.preferences.ApiPermissionsPreference
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.preferences.DeveloperModePreference
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.web.models.App
import com.ismartcoding.plain.web.models.TempValue
import com.ismartcoding.plain.web.models.toModel

@OptIn(ExperimentalEncodingApi::class)
fun SchemaBuilder.addAppSchema() {
    query("deviceInfo") {
        resolver { ->
            getDeviceInfo().toModel()
        }
    }
    query("battery") {
        resolver { ->
            getBattery().toModel()
        }
    }
    query("app") {
        resolver { ->
            val apiPermissions = ApiPermissionsPreference.getAsync()
            val grantedPermissions = Permission.entries.filter { apiPermissions.contains(it.name) && it.isGranted() }.toMutableList()
            if (Permission.RECORD_AUDIO.isGranted() && !grantedPermissions.contains(Permission.RECORD_AUDIO)) {
                grantedPermissions.add(Permission.RECORD_AUDIO)
            }
            App(
                clientId = TempData.clientId,
                usbConnected = isUsbConnected(),
                urlToken = Base64.encode(TempData.urlToken),
                httpPort = TempData.httpPort.value,
                httpsPort = TempData.httpsPort.value,
                appDir = appDir(),
                deviceName = TempData.deviceName.value,
                getBattery().level,
                getAppVersionCode().toInt(),
                getSdkInt(),
                buildChannel,
                grantedPermissions,
                AudioPlaylistPreference.getValueAsync().map { it.toModel() },
                TempData.audioPlayMode.value,
                AudioPlayingPreference.getValueAsync(),
                sdcardPath = getSDCardPath(),
                usbDiskPaths = getUsbDiskPaths(),
                internalStoragePath = getInternalStoragePath(),
                downloadsDir = getDownloadsDirPath(),
                developerMode = DeveloperModePreference.getAsync(),
                favoriteFolders = FavoriteFoldersPreference.getValueAsync().map { it.toModel() },
                debug = isDebugBuild(),
            )
        }
    }
    mutation("setTempValue") {
        resolver("key", "value") { key: String, value: String ->
            TempHelper.setValue(key, value)
            TempValue(key, value)
        }
    }
    mutation("relaunchApp") {
        resolver { ->
            sendEvent(RestartAppEvent())
            true
        }
    }
    mutation("openAccessibilitySettings") {
        resolver { ->
            sendEvent(HOpenAccessibilitySettingsEvent())
            true
        }
    }
    mutation("openWebSettings") {
        resolver { ->
            sendEvent(HOpenWebSettingsEvent())
            true
        }
    }
    mutation("setClip") {
        resolver("text") { text: String ->
            setClipboardText("text", text)
            true
        }
    }
    mutation("updateDeviceName") {
        resolver("name") { name: String ->
            DeviceNamePreference.putAsync(name)
            TempData.deviceName.value = name
            true
        }
    }
}
