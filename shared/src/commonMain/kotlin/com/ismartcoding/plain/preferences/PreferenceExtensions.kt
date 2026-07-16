package com.ismartcoding.plain.preferences

import androidx.datastore.preferences.core.Preferences
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.helpers.StringHelper

suspend fun AudioPlayModePreference.getValueAsync(): MediaPlayMode =
    MediaPlayMode.entries.getOrElse(getAsync()) { MediaPlayMode.REPEAT }

suspend fun AudioPlayModePreference.getValue(preferences: Preferences): MediaPlayMode =
    MediaPlayMode.entries.getOrElse(get(preferences)) { MediaPlayMode.REPEAT }

// ── ClientIdPreference ───────────────────────────────────────────────────────

suspend fun ClientIdPreference.ensureValueAsync(preferences: Preferences) {
    TempData.clientId = get(preferences)
    if (TempData.clientId.isEmpty()) {
        TempData.clientId = StringHelper.shortUUID()
        putAsync(TempData.clientId)
    }
}

// ── KeyStorePasswordPreference ───────────────────────────────────────────────

suspend fun KeyStorePasswordPreference.ensureValueAsync(preferences: Preferences) {
    var password = get(preferences)
    if (password.isEmpty()) {
        password = StringHelper.shortUUID()
        putAsync(password)
    }
}

suspend fun KeyStorePasswordPreference.resetAsync() {
    putAsync(StringHelper.shortUUID())
}

suspend fun MdnsHostnamePreference.ensureValueAsync(preferences: Preferences) {
    val stored = preferences[key]
    if (stored.isNullOrEmpty()) {
        val allowedChars = ('a'..'z').filter { it !in listOf('i', 'l', 'o', 'v') }
        val randomString = (1..2).map { allowedChars.random() }.joinToString("")
        val hostname = "$randomString.local"
        TempData.mdnsHostname = hostname
        putAsync(hostname)
    } else {
        TempData.mdnsHostname = stored
    }
}

// ── UrlTokenPreference ───────────────────────────────────────────────────────

suspend fun UrlTokenPreference.ensureValueAsync(preferences: Preferences) {
    val rotateOnRestart = RotateUrlTokenOnRestartPreference.get(preferences)
    if (rotateOnRestart) {
        val keyStr = com.ismartcoding.plain.platform.generateChaCha20Key()
        TempData.urlToken = com.ismartcoding.plain.helpers.Base64Lenient.decode(keyStr)
        putAsync(keyStr)
        return
    }
    val keyStr = get(preferences)
    if (keyStr.isEmpty()) {
        val newKeyStr = com.ismartcoding.plain.platform.generateChaCha20Key()
        TempData.urlToken = com.ismartcoding.plain.helpers.Base64Lenient.decode(newKeyStr)
        putAsync(newKeyStr)
    } else {
        TempData.urlToken = com.ismartcoding.plain.helpers.Base64Lenient.decode(keyStr)
    }
}

suspend fun UrlTokenPreference.resetAsync() {
    val keyStr = com.ismartcoding.plain.platform.generateChaCha20Key()
    TempData.urlToken = com.ismartcoding.plain.helpers.Base64Lenient.decode(keyStr)
    putAsync(keyStr)
}
