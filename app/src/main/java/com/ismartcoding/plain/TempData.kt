package com.ismartcoding.plain

import com.ismartcoding.plain.data.DNotification

object TempData {
    var webEnabled = false
    var demoMode = false
    var selectedBoxId = ""
    var endictShowWord = true
    var endictShowTranslation = true
    var clientId = ""
    var keyStorePassword = ""
    var httpPort: Int = 8080
    var httpsPort: Int = 8443
    var urlToken = "" // use to encrypt or decrypt params in url
    var chatItemsMigrated = false
    val notifications = mutableListOf<DNotification>()

    var audioSleepTimerFutureTime = 0L
}
