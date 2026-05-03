package com.ismartcoding.plain.ui.page.web

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import android.util.Base64
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
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
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSecurityPage(navController: NavHostController) {
    WebSettingsProvider {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val passwordType = LocalPasswordType.current
        val password = LocalPassword.current
        val authTwoFactor = LocalAuthTwoFactor.current
        val rotateUrlTokenOnRestart = LocalRotateUrlTokenOnRestart.current
        var urlToken by remember { mutableStateOf(Base64.encodeToString(TempData.urlToken, Base64.NO_WRAP)) }
        var keyStorePassword by remember { mutableStateOf("") }
        var sslSignature by remember { mutableStateOf("") }
        val editPassword = remember { mutableStateOf("") }

        LaunchedEffect(password) {
            if (editPassword.value != password) editPassword.value = password
            scope.launch(Dispatchers.IO) {
                keyStorePassword = KeyStorePasswordPreference.getAsync(context)
                try {
                    sslSignature = HttpServerManager.getSSLSignature(context, keyStorePassword).joinToString(" ") { "%02x".format(it).uppercase() }
                } catch (ex: Exception) {
                    LogCat.e("Failed to get SSL signature: ${ex.message}"); ex.printStackTrace()
                }
            }
        }

        PScaffold(
            topBar = { PTopAppBar(navController = navController, title = stringResource(R.string.security)) },
            content = { paddingValues ->
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item { TopSpace() }
                    item {
                        PCard {
                            PListItem(modifier = Modifier.clickable {
                                scope.launch(Dispatchers.IO) {
                                    PasswordTypePreference.putAsync(
                                        context,
                                        if (passwordType == PasswordType.NONE.value) PasswordType.FIXED.value else PasswordType.NONE.value
                                    )
                                }
                            }, title = stringResource(R.string.require_password)) {
                                PSwitch(activated = passwordType != PasswordType.NONE.value) {
                                    scope.launch(Dispatchers.IO) {
                                        PasswordTypePreference.putAsync(
                                            context,
                                            if (passwordType == PasswordType.NONE.value) PasswordType.FIXED.value else PasswordType.NONE.value
                                        )
                                    }
                                }
                            }
                            if (passwordType != PasswordType.NONE.value) {
                                PasswordTextField(
                                    value = editPassword.value, isChanged = { editPassword.value != password },
                                    onValueChange = { editPassword.value = it }, onConfirm = { scope.launch(Dispatchers.IO) { PasswordPreference.putAsync(context, it) } })
                                OutlinedButton(
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp),
                                    onClick = { scope.launch(Dispatchers.IO) { editPassword.value = HttpServerManager.resetPasswordAsync() } }) {
                                    Text(stringResource(R.string.generate_password))
                                }
                            }
                        }
                    }
                    item {
                        VerticalSpace(dp = 16.dp)
                        PCard {
                            PListItem(
                                modifier = Modifier.clickable { scope.launch(Dispatchers.IO) { AuthTwoFactorPreference.putAsync(context, !authTwoFactor) } },
                                title = stringResource(R.string.require_confirmation)
                            ) {
                                PSwitch(activated = authTwoFactor) { scope.launch(Dispatchers.IO) { AuthTwoFactorPreference.putAsync(context, it) } }
                            }
                        }
                        Tips(text = stringResource(R.string.two_factor_auth_tips)); VerticalSpace(dp = 24.dp)
                    }
                    item {
                        Subtitle(text = stringResource(R.string.https_certificate_signature))
                        ClipboardCard(label = stringResource(R.string.https_certificate_signature), text = sslSignature)
                        VerticalSpace(dp = 16.dp)
                        PFilledButton(text = stringResource(R.string.reset_ssl_certificate), type = ButtonType.DANGER, modifier = Modifier.padding(horizontal = 16.dp), onClick = {
                            scope.launch(Dispatchers.IO) {
                                DialogHelper.showLoading(); KeyStorePasswordPreference.resetAsync(context)
                                keyStorePassword = KeyStorePasswordPreference.getAsync(context)
                                HttpServerManager.generateSSLKeyStore(File(context.filesDir, Constants.KEY_STORE_FILE_NAME), keyStorePassword)
                                DialogHelper.hideLoading()
                                DialogHelper.showConfirmDialog("", LocaleHelper.getString(R.string.ssl_certificate_reset)) { sendEvent(RestartAppEvent()) }
                            }
                        })
                        VerticalSpace(dp = 24.dp); Subtitle(text = stringResource(R.string.url_token))
                        ClipboardCard(label = stringResource(R.string.url_token), text = urlToken)
                        Tips(text = stringResource(R.string.url_token_tips)); VerticalSpace(dp = 16.dp)
                        PCard {
                            PListItem(modifier = Modifier.clickable {
                                scope.launch(Dispatchers.IO) { RotateUrlTokenOnRestartPreference.putAsync(context, !rotateUrlTokenOnRestart) }
                            }, title = stringResource(R.string.rotate_url_token_on_restart)) {
                                PSwitch(activated = rotateUrlTokenOnRestart) {
                                    scope.launch(Dispatchers.IO) { RotateUrlTokenOnRestartPreference.putAsync(context, it) }
                                }
                            }
                        }
                        Tips(text = stringResource(R.string.rotate_url_token_on_restart_tips)); VerticalSpace(dp = 16.dp)
                        PFilledButton(text = stringResource(R.string.reset_token), type = ButtonType.DANGER, modifier = Modifier.padding(horizontal = 16.dp), onClick = {
                            scope.launch(Dispatchers.IO) {
                                UrlTokenPreference.resetAsync(context); urlToken = Base64.encodeToString(TempData.urlToken, Base64.NO_WRAP); DialogHelper.showMessage(R.string.the_token_is_reset)
                            }
                        })
                        BottomSpace(paddingValues)
                    }
                }
            })
    }
}
