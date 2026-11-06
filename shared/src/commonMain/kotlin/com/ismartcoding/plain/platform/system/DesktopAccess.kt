package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.preferences.PrefKeys

import com.ismartcoding.plain.Prefs

suspend fun setRemoteAccessEnabled(enabled: Boolean) {
    Prefs.set(PrefKeys.REMOTE_ACCESS, enabled)
}