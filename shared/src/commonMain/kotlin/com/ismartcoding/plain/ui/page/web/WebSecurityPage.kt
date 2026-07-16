package com.ismartcoding.plain.ui.page.web

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import androidx.compose.foundation.layout.fillMaxWidth
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.generateSSLKeyStore
import com.ismartcoding.plain.platform.getSSLSignature
import com.ismartcoding.plain.platform.resetPasswordAsync
import com.ismartcoding.plain.preferences.AuthTwoFactorPreference
import com.ismartcoding.plain.preferences.KeyStorePasswordPreference
import com.ismartcoding.plain.preferences.LocalAuthTwoFactor
import com.ismartcoding.plain.preferences.LocalPassword
import com.ismartcoding.plain.preferences.LocalPasswordType
import com.ismartcoding.plain.preferences.LocalRotateUrlTokenOnRestart
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.preferences.PasswordTypePreference
import com.ismartcoding.plain.preferences.RotateUrlTokenOnRestartPreference
import com.ismartcoding.plain.preferences.UrlTokenPreference
import com.ismartcoding.plain.preferences.WebSettingsProvider
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun WebSecurityPage(navController: NavHostController) {
    WebSettingsProvider {
        val scope = rememberCoroutineScope()
        val passwordType = LocalPasswordType.current
        val password = LocalPassword.current
        val authTwoFactor = LocalAuthTwoFactor.current
        val rotateUrlTokenOnRestart = LocalRotateUrlTokenOnRestart.current
        var urlToken by remember { mutableStateOf(Base64.encode(TempData.urlToken)) }
        var keyStorePassword by remember { mutableStateOf("") }
        var sslSignature by remember { mutableStateOf("") }
        val editPassword = remember { mutableStateOf("") }

        LaunchedEffect(password) {
            if (editPassword.value != password) editPassword.value = password
            scope.launch(Dispatchers.Default) {
                keyStorePassword = KeyStorePasswordPreference.getAsync()
                try {
                    sslSignature = getSSLSignature(keyStorePassword).joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
                } catch (ex: Exception) {
                    LogCat.e("Failed to get SSL signature: ${ex.message}"); ex.printStackTrace()
                }
            }
        }

        PScaffold(
            topBar = { PTopAppBar(navController = navController, title = stringResource(Res.string.security)) },
            content = { paddingValues ->
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item { TopSpace() }
                    item {
                        PCard {
                            PListItem(modifier = Modifier.clickable {
                                scope.launch(Dispatchers.Default) {
                                    PasswordTypePreference.putAsync(
                                        if (passwordType == PasswordType.NONE.value) PasswordType.FIXED.value else PasswordType.NONE.value
                                    )
                                }
                            }, title = stringResource(Res.string.require_password)) {
                                PSwitch(activated = passwordType != PasswordType.NONE.value) {
                                    scope.launch(Dispatchers.Default) {
                                        PasswordTypePreference.putAsync(
                                            if (passwordType == PasswordType.NONE.value) PasswordType.FIXED.value else PasswordType.NONE.value
                                        )
                                    }
                                }
                            }
                            if (passwordType != PasswordType.NONE.value) {
                                PasswordTextField(
                                    value = editPassword.value, isChanged = { editPassword.value != password },
                                    onValueChange = { editPassword.value = it }, onConfirm = { scope.launch(Dispatchers.Default) { PasswordPreference.putAsync(it) } })
                                PFilledButton(
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp),
                                    text = stringResource(Res.string.generate_password),
                                    buttonSize = ButtonSize.MEDIUM,
                                    onClick = { scope.launch(Dispatchers.Default) { editPassword.value = resetPasswordAsync() } })
                            }
                        }
                    }
                    item {
                        VerticalSpace(dp = 16.dp)
                        PCard {
                            PListItem(
                                modifier = Modifier.clickable { scope.launch(Dispatchers.Default) { AuthTwoFactorPreference.putAsync(!authTwoFactor) } },
                                title = stringResource(Res.string.require_confirmation)
                            ) {
                                PSwitch(activated = authTwoFactor) { scope.launch(Dispatchers.Default) { AuthTwoFactorPreference.putAsync(it) } }
                            }
                        }
                        Tips(text = stringResource(Res.string.two_factor_auth_tips)); VerticalSpace(dp = 24.dp)
                    }
                    item {
                        Subtitle(text = stringResource(Res.string.https_certificate_signature))
                        ClipboardCard(label = stringResource(Res.string.https_certificate_signature), text = sslSignature)
                        VerticalSpace(dp = 16.dp)
                        PFilledButton(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            text = stringResource(Res.string.reset_ssl_certificate),
                            type = ButtonType.DANGER, onClick = {
                                scope.launch(Dispatchers.Default) {
                                    DialogHelper.showLoading()
                                    KeyStorePasswordPreference.resetAsync()
                                    keyStorePassword = KeyStorePasswordPreference.getAsync()
                                    generateSSLKeyStore(keyStorePassword)
                                    DialogHelper.hideLoading()
                                    DialogHelper.showConfirmDialog("", LocaleHelper.getStringAsync(Res.string.ssl_certificate_reset)) { sendEvent(RestartAppEvent()) }
                                }
                            })
                        VerticalSpace(dp = 24.dp); Subtitle(text = stringResource(Res.string.url_token))
                        ClipboardCard(label = stringResource(Res.string.url_token), text = urlToken)
                        Tips(text = stringResource(Res.string.url_token_tips)); VerticalSpace(dp = 16.dp)
                        PCard {
                            PListItem(modifier = Modifier.clickable {
                                scope.launch(Dispatchers.Default) { RotateUrlTokenOnRestartPreference.putAsync(!rotateUrlTokenOnRestart) }
                            }, title = stringResource(Res.string.rotate_url_token_on_restart)) {
                                PSwitch(activated = rotateUrlTokenOnRestart) {
                                    scope.launch(Dispatchers.Default) { RotateUrlTokenOnRestartPreference.putAsync(it) }
                                }
                            }
                        }
                        Tips(text = stringResource(Res.string.rotate_url_token_on_restart_tips)); VerticalSpace(dp = 16.dp)
                        PFilledButton(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            text = stringResource(Res.string.reset_token),
                            type = ButtonType.DANGER,
                            onClick = {
                                scope.launch(Dispatchers.Default) {
                                    UrlTokenPreference.resetAsync()
                                    urlToken = Base64.encode(TempData.urlToken)
                                    DialogHelper.showMessage(Res.string.the_token_is_reset)
                                }
                            })
                        BottomSpace(paddingValues)
                    }
                }
            })
    }
}
