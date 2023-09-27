package com.ismartcoding.plain.data.preference

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
    val httpPort: Int,
    val httpsPort: Int,
    val authTwoFactor: Boolean,
    val authDevToken: String,
    val apiPermissions: Set<String>,
)

val LocalPasswordType = compositionLocalOf { PasswordTypePreference.default }
val LocalPassword = compositionLocalOf { PasswordPreference.default }
val LocalHttpPort = compositionLocalOf { HttpPortPreference.default }
val LocalHttpsPort = compositionLocalOf { HttpsPortPreference.default }
val LocalAuthTwoFactor = compositionLocalOf { AuthTwoFactorPreference.default }
val LocalApiPermissions = compositionLocalOf { ApiPermissionsPreference.default }
val LocalAuthDevToken = compositionLocalOf { AuthDevTokenPreference.default }

@Composable
fun WebSettingsProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val defaultSettings =
        WebSettings(
            passwordType = PasswordTypePreference.default,
            password = PasswordPreference.default,
            httpPort = HttpPortPreference.default,
            httpsPort = HttpsPortPreference.default,
            authTwoFactor = AuthTwoFactorPreference.default,
            authDevToken = AuthDevTokenPreference.default,
            apiPermissions = ApiPermissionsPreference.default,
        )
    val settings =
        remember {
            context.dataStore.data.map {
                WebSettings(
                    passwordType = PasswordTypePreference.get(it),
                    password = PasswordPreference.get(it),
                    httpPort = HttpPortPreference.get(it),
                    httpsPort = HttpsPortPreference.get(it),
                    authTwoFactor = AuthTwoFactorPreference.get(it),
                    authDevToken = AuthDevTokenPreference.get(it),
                    apiPermissions = ApiPermissionsPreference.get(it),
                )
            }
        }.collectAsStateValue(
            initial = defaultSettings,
        )

    CompositionLocalProvider(
        LocalPasswordType provides settings.passwordType,
        LocalPassword provides settings.password,
        LocalHttpPort provides settings.httpPort,
        LocalHttpsPort provides settings.httpsPort,
        LocalAuthTwoFactor provides settings.authTwoFactor,
        LocalAuthDevToken provides settings.authDevToken,
        LocalApiPermissions provides settings.apiPermissions,
    ) {
        content()
    }
}
