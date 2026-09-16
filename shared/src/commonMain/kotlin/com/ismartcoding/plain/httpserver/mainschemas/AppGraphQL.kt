package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.enums.AccessFeatureType
import com.ismartcoding.plain.enums.AppChannelType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.events.HOpenAccessibilitySettingsEvent
import com.ismartcoding.plain.events.HOpenWebSettingsEvent
import com.ismartcoding.plain.features.getGrantedWebPermissionsAsync
import com.ismartcoding.plain.platform.appDir
import com.ismartcoding.plain.platform.getBattery
import com.ismartcoding.plain.platform.getDeviceInfo
import com.ismartcoding.plain.platform.getDownloadsDirPath
import com.ismartcoding.plain.platform.getInternalStoragePath
import com.ismartcoding.plain.platform.getSDCardPath
import com.ismartcoding.plain.platform.getUsbDiskPaths
import com.ismartcoding.plain.platform.isUsbConnected
import com.ismartcoding.plain.platform.isDebugBuild
import com.ismartcoding.plain.platform.getAppVersionName
import com.ismartcoding.plain.platform.getSdkInt
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.helpers.TempHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.preferences.ClipboardSyncPreference
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.preferences.DeveloperModePreference
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.httpserver.models.App
import com.ismartcoding.plain.httpserver.models.Battery
import com.ismartcoding.plain.httpserver.models.DeviceInfo
import com.ismartcoding.plain.httpserver.models.TempValue
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.platform.getDeviceType

@GraphQLQuery
suspend fun deviceInfo(): DeviceInfo {
    return getDeviceInfo().toModel()
}

@GraphQLQuery
suspend fun battery(): Battery {
    return getBattery().toModel()
}

@OptIn(ExperimentalEncodingApi::class)
@GraphQLQuery
suspend fun app(): App {
    val grantedPermissions = getGrantedWebPermissionsAsync()
    return App(
        clientId = TempData.clientId,
        usbConnected = isUsbConnected(),
        urlToken = Base64.encode(TempData.urlToken),
        httpPort = TempData.httpPort.value,
        httpsPort = TempData.httpsPort.value,
        appDir = appDir(),
        deviceName = TempData.deviceName.value,
        deviceType = getDeviceType(),
        getBattery().level,
        getAppVersionName(),
        getSdkInt(),
        AppChannelType.fromString(buildChannel),
        grantedPermissions,
        AudioPlaylistPreference.getValueAsync().map { it.toModel() },
        TempData.audioPlayMode.value,
        AudioPlayingPreference.getValueAsync(),
        sdcardPath = getSDCardPath(),
        usbDiskPaths = getUsbDiskPaths(),
        internalStoragePath = getInternalStoragePath(),
        downloadsDir = getDownloadsDirPath(),
        developerMode = DeveloperModePreference.getAsync(),
        clipboardSync = ClipboardSyncPreference.getAsync(),
        favoriteFolders = FavoriteFoldersPreference.getValueAsync().map { it.toModel() },
        debug = isDebugBuild(),
    )
}

@GraphQLMutation
suspend fun setTempValue(key: String, value: String): TempValue {
    TempHelper.setValue(key, value)
    return TempValue(key, value)
}

@GraphQLMutation
suspend fun relaunchApp(): Boolean {
    sendEvent(RestartAppEvent())
    return true
}

@GraphQLMutation
suspend fun openAccessibilitySettings(): Boolean {
    sendEvent(HOpenAccessibilitySettingsEvent())
    return true
}

@GraphQLMutation
suspend fun openWebSettings(feature: AccessFeatureType? = null): Boolean {
    sendEvent(HOpenWebSettingsEvent(feature))
    return true
}

@GraphQLMutation
suspend fun setClip(text: String): Boolean {
    setClipboardText("text", text)
    return true
}

@GraphQLMutation
suspend fun updateDeviceName(name: String): Boolean {
    DeviceNamePreference.putAsync(name)
    TempData.deviceName.value = name
    MdnsDiscoverManager.updateAdvertisedService()
    return true
}

fun SchemaBuilder.addAppSchema() {
}
