package com.ismartcoding.plain.preferences

import android.content.Context
import android.os.LocaleList
import android.util.Base64
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.Preferences
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.Language
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.helpers.PhoneHelper
import java.util.Locale

// ── DarkThemePreference ──────────────────────────────────────────────────────
// DarkTheme type ≠ Int (stored type), so this extension is unambiguous.

fun DarkThemePreference.setDarkMode(theme: DarkTheme) {
    when (theme) {
        DarkTheme.ON -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        DarkTheme.OFF -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}

suspend fun DarkThemePreference.putAsync(value: DarkTheme) {
    putAsync(value.value)   // calls the base member putAsync(Int)
    setDarkMode(value)
}

fun LanguagePreference.getLocale(preferences: Preferences): Locale? {
    return parseLocale(get(preferences))
}

suspend fun LanguagePreference.getLocaleAsync(): Locale? {
    return parseLocale(getAsync())
}

private fun parseLocale(value: String): Locale? {
    if (value.isEmpty()) return null
    val s = value.split("-")
    return if (s.size > 1) Locale(s[0], s[1]) else Locale(value)
}

// Context is needed for Language.setLocale; Locale? ≠ String so extension is unambiguous.
suspend fun LanguagePreference.putAsync(locale: Locale?) {
    var value = ""
    if (locale != null) {
        value = locale.language
        if (locale.country.isNotEmpty()) value += "-${locale.country}"
    }
    putAsync(value)   // calls the base member putAsync(String)
    Language.setLocale(MainApp.instance, locale ?: LocaleList.getDefault().get(0))
}

// ── WebPreference ────────────────────────────────────────────────────────────
// Note: TempData.webEnabled must be updated at call sites after putAsync.

// ── HttpsPreference ──────────────────────────────────────────────────────────
// Note: TempData.webHttps must be updated at call sites after putAsync.

// ── AudioPlayModePreference ──────────────────────────────────────────────────
// MediaPlayMode ≠ Int, no ambiguity.

suspend fun AudioPlayModePreference.putAsync(value: MediaPlayMode) {
    putAsync(value.ordinal)   // calls base member putAsync(Int)
    TempData.audioPlayMode.value = value
}

suspend fun AudioPlayModePreference.getValueAsync(): MediaPlayMode =
    MediaPlayMode.entries.getOrElse(getAsync()) { MediaPlayMode.REPEAT }

// ── ApiPermissionsPreference ─────────────────────────────────────────────────
// 2-param overload, unambiguous.

suspend fun ApiPermissionsPreference.putAsync(permission: Permission, enable: Boolean) {
    val permissions = getAsync().toMutableSet()
    if (enable) permissions.add(permission.name) else permissions.remove(permission.name)
    putAsync(permissions)   // calls base member putAsync(Set<String>)
}

// ── AdbTokenPreference ───────────────────────────────────────────────────────

suspend fun AdbTokenPreference.ensureValueAsync(preferences: Preferences) {
    TempData.adbToken = get(preferences)
    if (TempData.adbToken.isEmpty()) {
        TempData.adbToken = CryptoHelper.randomPassword(32)
        putAsync(TempData.adbToken)
    }
}

suspend fun AdbTokenPreference.resetAsync() {
    TempData.adbToken = CryptoHelper.randomPassword(32)
    putAsync(TempData.adbToken)
}

// ── UrlTokenPreference ───────────────────────────────────────────────────────

suspend fun UrlTokenPreference.ensureValueAsync(preferences: Preferences) {
    val rotateOnRestart = RotateUrlTokenOnRestartPreference.get(preferences)
    if (rotateOnRestart) {
        val keyStr = CryptoHelper.generateChaCha20Key()
        TempData.urlToken = Base64.decode(keyStr, Base64.NO_WRAP)
        putAsync(keyStr)
        return
    }
    val keyStr = get(preferences)
    if (keyStr.isEmpty()) {
        val newKeyStr = CryptoHelper.generateChaCha20Key()
        TempData.urlToken = Base64.decode(newKeyStr, Base64.NO_WRAP)
        putAsync(newKeyStr)
    } else {
        TempData.urlToken = Base64.decode(keyStr, Base64.NO_WRAP)
    }
}

suspend fun UrlTokenPreference.resetAsync() {
    val keyStr = CryptoHelper.generateChaCha20Key()
    TempData.urlToken = Base64.decode(keyStr, Base64.NO_WRAP)
    putAsync(keyStr)
}

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

// ── MdnsHostnamePreference ───────────────────────────────────────────────────

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
