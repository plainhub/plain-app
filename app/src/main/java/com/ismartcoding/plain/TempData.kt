package com.ismartcoding.plain

import android.app.Notification
import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.enums.MediaPlayMode

object TempData {
    var webEnabled = false
    var webHttps = false
    var clientId = ""
    var httpPort: Int = 8080
    var httpsPort: Int = 8443
    var urlToken = "" // use to encrypt or decrypt params in url
    var mdnsHostname = "plainapp.local" // mDNS hostname for local network discovery
    val notifications = mutableListOf<DNotification>()
    // Stores notification actions (including RemoteInput reply actions) keyed by notification id
    val notificationActions = mutableMapOf<String, Array<out Notification.Action>>()
    var audioPlayMode = MediaPlayMode.REPEAT

    var audioSleepTimerFutureTime = 0L
    var audioPlayPosition = 0L // audio play position in milliseconds

    // The peer ID whose ChatPage is currently open; empty when no peer chat is active.
    var activeChatPeerId = ""
}
