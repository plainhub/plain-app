package com.ismartcoding.plain

import com.ismartcoding.lib.helpers.CryptoHelper

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
    val urlToken = CryptoHelper.generateAESKey() // use to encrypt or decrypt params in url
    var chatItemsMigrated = false
}