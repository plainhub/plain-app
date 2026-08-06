package com.ismartcoding.plain

import androidx.datastore.preferences.core.Preferences
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.events.StartNearbyServiceEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.getDeviceName
import com.ismartcoding.plain.features.dlna.startDlnaRenderer
import com.ismartcoding.plain.preferences.AudioPlaybackSpeedPreference
import com.ismartcoding.plain.preferences.AudioPlayModePreference
import com.ismartcoding.plain.preferences.ClientIdPreference
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.preferences.DlnaPreference
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.preferences.HttpsPreference
import com.ismartcoding.plain.preferences.KeyStorePasswordPreference
import com.ismartcoding.plain.preferences.MdnsHostnamePreference
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.preferences.SignatureKeyPreference
import com.ismartcoding.plain.preferences.UrlTokenPreference
import com.ismartcoding.plain.preferences.DesktopAccessPreference
import com.ismartcoding.plain.preferences.DeveloperModePreference
import com.ismartcoding.plain.preferences.ServicePreference
import com.ismartcoding.plain.preferences.getPreferencesAsync
import com.ismartcoding.plain.httpserver.HttpServerManager

/**
 * Shared preference and TempData initialization, called by both Android
 * (`MainAppHelper`) and iOS (`MainViewController`) during app startup.
 *
 * Platform-specific initialization (Android: media duration cache, PeerCacher,
 * FeedFetchWorker, etc.) stays in the platform modules and is called before
 * or after this function as needed.
 */
suspend fun initCommonPreferences(): Preferences {
    val preferences = getPreferencesAsync()
    TempData.dlnaEnabled.value = DlnaPreference.get(preferences)
    TempData.nearbyDiscoverable = NearbyDiscoverablePreference.getAsync()
    TempData.developerMode = DeveloperModePreference.get(preferences)
    SignatureKeyPreference.ensureKeyPairAsync()
    TempData.desktopAccessEnabled.value = DesktopAccessPreference.get(preferences)
    TempData.serviceEnabled.value = ServicePreference.get(preferences)
    TempData.webHttps.value = HttpsPreference.get(preferences)
    TempData.httpPort.value = HttpPortPreference.get(preferences)
    TempData.httpsPort.value = HttpsPortPreference.get(preferences)
    TempData.audioPlayMode.value = AudioPlayModePreference.getValue(preferences)
    TempData.audioPlaybackSpeed.value = AudioPlaybackSpeedPreference.getValue(preferences)
    ClientIdPreference.ensureValueAsync(preferences)
    TempData.deviceName.value = DeviceNamePreference.get(preferences).ifEmpty { getDeviceName() }
    KeyStorePasswordPreference.ensureValueAsync(preferences)
    UrlTokenPreference.ensureValueAsync(preferences)
    MdnsHostnamePreference.ensureValueAsync(preferences)
    if (PasswordPreference.get(preferences).isEmpty()) {
        HttpServerManager.resetPasswordAsync()
    }
    PeerCacher.load()
    ChannelCacher.load()
    ChatCacher.load()
    HttpServerManager.clientTsInterval()
    sendEvent(StartNearbyServiceEvent())
    HttpServerManager.loadTokenCache()
    if (TempData.canDLNAAccess()) {
        startDlnaRenderer()
    }
    LogCat.d("initCommonPreferences: clientId=${TempData.clientId}, deviceName=${TempData.deviceName.value}")
    return preferences
}
