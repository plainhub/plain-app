package com.ismartcoding.plain

import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.enums.MediaPlayMode

object TempData {
    var webEnabled = false
    var webHttps = false
    var clientId = ""
    var httpPort: Int = 8080
    var httpsPort: Int = 8443
    var urlToken = "" // use to encrypt or decrypt params in url
    val notifications = mutableListOf<DNotification>()
    var audioPlayMode = MediaPlayMode.REPEAT

    var audioSleepTimerFutureTime = 0L
    var audioPlayPosition = 0L // audio play position in milliseconds
}
