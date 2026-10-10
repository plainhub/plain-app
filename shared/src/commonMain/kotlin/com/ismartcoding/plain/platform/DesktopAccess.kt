package com.ismartcoding.plain.platform

import com.ismartcoding.plain.preferences.PreferencesClient
import com.ismartcoding.plain.preferences.UserSettingsPatch

suspend fun setDesktopAccessEnabled(enabled: Boolean) {
    PreferencesClient.local.patchUser(UserSettingsPatch(desktopAccess = enabled))
}
