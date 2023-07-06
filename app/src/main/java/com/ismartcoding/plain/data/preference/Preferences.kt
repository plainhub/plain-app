package com.ismartcoding.plain.data.preference

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.enums.DarkTheme
import com.ismartcoding.plain.data.enums.Language
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.features.Permission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object PasswordPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("password")
}

object PasswordTypePreference : BasePreference<Int>() {
    override val default = PasswordType.RANDOM.value
    override val key = intPreferencesKey("password_type")

    fun put(context: Context, scope: CoroutineScope, value: PasswordType) {
        put(context, scope, value.value)
    }
}

object AuthTwoFactorPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("auth_two_factor")
}

object AuthDevTokenPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("auth_dev_token")
}

object ApiPermissionsPreference : BasePreference<Set<String>>() {
    override val default = setOf<String>()
    override val key = stringSetPreferencesKey("api_permissions")

    fun put(context: Context, scope: CoroutineScope, permission: Permission, enable: Boolean) {
        val permissions = get(context).toMutableSet()
        if (enable) {
            permissions.add(permission.name)
        } else {
            permissions.remove(permission.name)
        }
        put(context, scope, permissions)
    }
}


object HttpPortPreference : BasePreference<Int>() {
    override val default = 8080
    override val key = intPreferencesKey("http_port")
}

object HttpsPortPreference : BasePreference<Int>() {
    override val default = 8443
    override val key = intPreferencesKey("https_port")
}

object DarkThemePreference : BasePreference<Int>() {
    override val default = DarkTheme.UseDeviceTheme.value
    override val key = intPreferencesKey("dark_theme")

    fun put(context: Context, scope: CoroutineScope, value: DarkTheme) {
        put(context, scope, value.value)
        setDarkMode(value)
    }

    fun setDarkMode(theme: DarkTheme) {
        when (theme) {
            DarkTheme.ON -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            DarkTheme.OFF -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}

object CustomPrimaryColorPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("custom_primary_color")
}

object AmoledDarkThemePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("amoled_dark_theme")
}

object ThemeIndexPreference : BasePreference<Int>() {
    override val default = 5
    override val key = intPreferencesKey("theme_index")
}

object KeepScreenOnPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("keep_screen_on")
}

object SystemScreenTimeoutPreference : BasePreference<Int>() {
    override val default = 0
    override val key = intPreferencesKey("system_screen_timeout")
}

object LanguagePreference : BasePreference<Int>() {
    override val default = Language.UseDeviceLanguage.value
    override val key = intPreferencesKey("language")

    fun put(context: Context, scope: CoroutineScope, value: Language) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(
                key,
                value.value
            )
            value.setLocale(context)
        }
    }
}

object WebPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("web")
}

object ExchangeRatePreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("exchange")

    fun getConfig(preferences: Preferences): ExchangeConfig {
        val str = get(preferences)
        if (str.isEmpty()) {
            return ExchangeConfig()
        }
        return Json.decodeFromString(str)
    }

    fun put(context: Context, scope: CoroutineScope, value: ExchangeConfig) {
        put(context, scope, jsonEncode(value))
    }
}