package com.ismartcoding.plain

import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.enums.MediaPlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

object TempData {
    val webEnabled = MutableStateFlow(false)
    val webHttps = MutableStateFlow(false)
    val httpPort = MutableStateFlow(8080)
    val httpsPort = MutableStateFlow(8443)
    var clientId = ""
    val deviceName = MutableStateFlow("")
    var urlToken = ByteArray(0) // use to encrypt or decrypt params in url (kept as raw bytes to avoid base64 decode on every encrypt/decrypt)
    var mdnsHostname = "plainapp.local" // mDNS hostname for local network discovery

    val audioPlayMode = MutableStateFlow(MediaPlayMode.REPEAT)

    var adbToken = "" // in-memory cache of the ADB automation token

    var nearbyDiscoverable = false

    val awareRunning = MutableStateFlow(false)

    var audioSleepTimerFutureTime = 0L
    var audioPlayPosition = 0L // audio play position in milliseconds
    // mediaId -> playback position in milliseconds; pre-loaded from DB on startup as cache
    val videoPlayProgressMap = mutableMapOf<String, Long>()

    // Encoded target id of the chat page currently in the foreground. Set by
    // ChatPageEffects so the chat receiver can suppress notifications for the
    // active conversation. Format: "peer:<id>" / "channel:<id>" / "local".
    var activeToId = ""
}
