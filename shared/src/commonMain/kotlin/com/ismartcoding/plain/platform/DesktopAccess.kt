package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.session.closeAllWsSessions
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val desktopAccessMutex = Mutex()

suspend fun setDesktopAccessEnabled(enabled: Boolean) = desktopAccessMutex.withLock {
    UserPrefs.desktopAccess.value = enabled
    if (!enabled) closeAllWsSessions()
}
