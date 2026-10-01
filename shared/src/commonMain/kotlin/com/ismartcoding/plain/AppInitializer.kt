package com.ismartcoding.plain


import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.events.StartNearbyServiceEvent
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.getDeviceName
import com.ismartcoding.plain.features.dlna.startDlnaRenderer
import com.ismartcoding.plain.helpers.AppFileRealPathMigration
import com.ismartcoding.plain.preferences.*
import com.ismartcoding.plain.httpserver.HttpServerManager

/**
 * Shared preference and TempData initialization, called by both Android
 * (`MainAppHelper`) and iOS (`MainViewController`) during app startup.
 *
 * Platform-specific initialization (Android: media duration cache, PeerCacher,
 * FeedFetchWorker, etc.) stays in the platform modules and is called before
 * or after this function as needed.
 */
suspend fun initCommonPreferences() {
    SystemPrefs.ensureSignatureKeyPair()
    SystemPrefs.ensureClientId()
    TempData.deviceName.value = UserPrefs.deviceName.value.ifEmpty { getDeviceName() }
    SystemPrefs.ensureKeyStorePassword()
    SystemPrefs.ensureUrlToken()
    SystemPrefs.ensureMdnsHostname()
    if (SystemPrefs.password.value.isEmpty()) {
        HttpServerManager.resetPasswordAsync()
    }
    PeerCacher.load()
    ChannelCacher.load()
    ChatCacher.load()
    HttpServerManager.clientTsInterval()
    sendEvent(StartNearbyServiceEvent())
    if (TempData.canDLNAAccess()) {
        startDlnaRenderer()
    }
    if (!SystemPrefs.appFileRealPathMigrated.value) {
        AppFileRealPathMigration.run()
        SystemPrefs.appFileRealPathMigrated.value = true
    }
    AudioQueueManager.ensureMigrated()
    LogCat.d("initCommonPreferences: clientId=${TempData.clientId}, deviceName=${TempData.deviceName.value}")
}
