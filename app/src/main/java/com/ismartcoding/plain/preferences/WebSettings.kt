package com.ismartcoding.plain.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import kotlinx.coroutines.flow.map

data class WebSettings(
    val passwordType: Int,
    val password: String,
    val authTwoFactor: Boolean,
    val authDevToken: String,
    val keepAwake: Boolean,
    val apiPermissions: Set<String>,
)

val LocalPasswordType = compositionLocalOf { PasswordTypePreference.default }
val LocalPassword = compositionLocalOf { PasswordPreference.default }
val LocalAuthTwoFactor = compositionLocalOf { AuthTwoFactorPreference.default }
val LocalApiPermissions = compositionLocalOf { ApiPermissionsPreference.default }
val LocalAuthDevToken = compositionLocalOf { AuthDevTokenPreference.default }
val LocalKeepAwake = compositionLocalOf { KeepAwakePreference.default }

@Composable
fun WebSettingsProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val defaultSettings =
        WebSettings(
            passwordType = PasswordTypePreference.default,
            password = PasswordPreference.default,
            authTwoFactor = AuthTwoFactorPreference.default,
            authDevToken = AuthDevTokenPreference.default,
            keepAwake = KeepAwakePreference.default,
            apiPermissions = ApiPermissionsPreference.default,
        )
    val settings =
        remember {
            context.dataStore.dataFlow.map {
                WebSettings(
                    passwordType = PasswordTypePreference.get(it),
                    password = PasswordPreference.get(it),
                    authTwoFactor = AuthTwoFactorPreference.get(it),
                    authDevToken = AuthDevTokenPreference.get(it),
                    keepAwake = KeepAwakePreference.get(it),
                    apiPermissions = ApiPermissionsPreference.get(it),
                )
            }
        }.collectAsStateValue(
            initial = defaultSettings,
        )

    CompositionLocalProvider(
        LocalPasswordType provides settings.passwordType,
        LocalPassword provides settings.password,
        LocalAuthTwoFactor provides settings.authTwoFactor,
        LocalAuthDevToken provides settings.authDevToken,
        LocalKeepAwake provides settings.keepAwake,
        LocalApiPermissions provides settings.apiPermissions,
    ) {
        content()
    }
}
