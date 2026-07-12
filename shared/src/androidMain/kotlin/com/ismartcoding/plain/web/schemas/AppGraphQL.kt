package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.getAppVersionCode
import com.ismartcoding.plain.isDebugBuild
import com.ismartcoding.plain.preferences.*

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Environment
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.extensions.appDir
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.events.HOpenAccessibilitySettingsEvent
import com.ismartcoding.plain.events.HOpenWebSettingsEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.helpers.DeviceInfoHelper
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.TempHelper
import com.ismartcoding.plain.preferences.ApiPermissionsPreference
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.preferences.DeveloperModePreference
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.receivers.BatteryReceiver
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.web.models.App
import com.ismartcoding.plain.web.models.TempValue
import com.ismartcoding.plain.web.models.toModel

@OptIn(ExperimentalEncodingApi::class)
fun SchemaBuilder.addAppSchema() {
    query("deviceInfo") {
        resolver { ->
            val context = appContext
            DeviceInfoHelper.getDeviceInfo(context).toModel()
        }
    }
    query("battery") {
        resolver { ->
            BatteryReceiver.get(appContext).toModel()
        }
    }
    query("app") {
        resolver { ->
            val context = appContext
            val apiPermissions = ApiPermissionsPreference.getAsync()
            val grantedPermissions = Permission.entries.filter { apiPermissions.contains(it.name) && it.isGranted() }.toMutableList()
            if (Permission.RECORD_AUDIO.isGranted() && !grantedPermissions.contains(Permission.RECORD_AUDIO)) {
                grantedPermissions.add(Permission.RECORD_AUDIO)
            }
            App(
                clientId = TempData.clientId,
                usbConnected = PlugInControlReceiver.isUSBConnected(context),
                urlToken = Base64.encode(TempData.urlToken),
                httpPort = TempData.httpPort.value,
                httpsPort = TempData.httpsPort.value,
                appDir = context.appDir(),
                deviceName = TempData.deviceName.value,
                PhoneHelper.getBatteryPercentage(context),
                getAppVersionCode().toInt(),
                Build.VERSION.SDK_INT,
                buildChannel,
                grantedPermissions,
                AudioPlaylistPreference.getValueAsync().map { it.toModel() },
                TempData.audioPlayMode.value,
                AudioPlayingPreference.getValueAsync(),
                sdcardPath = FileSystemHelper.getSDCardPath(context),
                usbDiskPaths = FileSystemHelper.getUsbDiskPaths(),
                internalStoragePath = FileSystemHelper.getInternalStoragePath(),
                downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
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
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", text)
            clipboard.setPrimaryClip(clip)
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
