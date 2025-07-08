package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.sendEvent
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
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.preferences.PasswordTypePreference
import com.ismartcoding.plain.preferences.UrlTokenPreference
import com.ismartcoding.plain.preferences.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.ClipboardCard
import com.ismartcoding.plain.ui.base.PBlockButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
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
        var urlToken by remember { mutableStateOf(TempData.urlToken) }
        var keyStorePassword by remember { mutableStateOf("") }
        var sslSignature by remember { mutableStateOf("") }

        val editPassword = remember { mutableStateOf("") }
        LaunchedEffect(password) {
            if (editPassword.value != password) {
                editPassword.value = password
            }
            scope.launch(Dispatchers.IO) {
                keyStorePassword = KeyStorePasswordPreference.getAsync(context)
                sslSignature = HttpServerManager.getSSLSignature(context, keyStorePassword).joinToString(" ") {
                    "%02x".format(it).uppercase()
                }
            }
        }

        PScaffold(
            topBar = {
                PTopAppBar(navController = navController, title = stringResource(R.string.security))
            },
            content = { paddingValues ->
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item {
                        TopSpace()
                    }
                    item {
                        PCard {
                            PListItem(
                                modifier = Modifier.clickable {
                                    scope.launch(Dispatchers.IO) {
                                        PasswordTypePreference.putAsync(context, if (passwordType == PasswordType.NONE.value) PasswordType.FIXED.value else PasswordType.NONE.value)
                                    }
                                },
                                title = stringResource(R.string.require_password),
                            ) {
                                PSwitch(
                                    activated = passwordType != PasswordType.NONE.value,
                                ) {
                                    scope.launch(Dispatchers.IO) {
                                        PasswordTypePreference.putAsync(context, if (passwordType == PasswordType.NONE.value) PasswordType.FIXED.value else PasswordType.NONE.value)
                                    }
                                }
                            }
                            if (passwordType != PasswordType.NONE.value) {
                                PasswordTextField(
                                    value = editPassword.value,
                                    isChanged = {
                                        editPassword.value != password
                                    },
                                    onValueChange = {
                                        editPassword.value = it
                                    },
                                    onConfirm = {
                                        scope.launch(Dispatchers.IO) {
                                            PasswordPreference.putAsync(context, it)
                                        }
                                    },
                                )
                                OutlinedButton(
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp),
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            editPassword.value = HttpServerManager.resetPasswordAsync()
                                        }
                                    }) {
                                    Text(text = stringResource(id = R.string.generate_password))
                                }
                            }
                        }
                    }
                    item {
                        VerticalSpace(dp = 16.dp)
                        PCard {
                            PListItem(
                                modifier = Modifier.clickable {
                                    scope.launch(Dispatchers.IO) {
                                        AuthTwoFactorPreference.putAsync(context, !authTwoFactor)
                                    }
                                },
                                title = stringResource(R.string.require_confirmation),
                            ) {
                                PSwitch(
                                    activated = authTwoFactor,
                                ) {
                                    scope.launch(Dispatchers.IO) {
                                        AuthTwoFactorPreference.putAsync(context, it)
                                    }
                                }
                            }
                        }
                        Tips(text = stringResource(R.string.two_factor_auth_tips))
                        VerticalSpace(dp = 24.dp)
                    }
                    item {
                        Subtitle(text = stringResource(id = R.string.https_certificate_signature))
                        ClipboardCard(
                            label =
                                stringResource(
                                    id = R.string.https_certificate_signature,
                                ),
                            sslSignature,
                        )
                        VerticalSpace(dp = 16.dp)
                        PBlockButton(
                            text = stringResource(id = R.string.reset_ssl_certificate),
                            type = ButtonType.DANGER,
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    DialogHelper.showLoading()
                                    KeyStorePasswordPreference.resetAsync(context)
                                    keyStorePassword = KeyStorePasswordPreference.getAsync(context)
                                    val file = File(context.filesDir, Constants.KEY_STORE_FILE_NAME)
                                    HttpServerManager.generateSSLKeyStore(file, keyStorePassword)
                                    DialogHelper.hideLoading()
                                    DialogHelper.showConfirmDialog("", LocaleHelper.getString(R.string.ssl_certificate_reset)) {
                                        sendEvent(RestartAppEvent())
                                    }
                                }
                            },
                        )
                        VerticalSpace(dp = 24.dp)
                        Subtitle(text = stringResource(id = R.string.url_token))
                        ClipboardCard(label = stringResource(id = R.string.url_token), urlToken)
                        Tips(text = stringResource(id = R.string.url_token_tips))
                        VerticalSpace(dp = 16.dp)
                        PBlockButton(
                            text = stringResource(id = R.string.reset_token),
                            type = ButtonType.DANGER,
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    UrlTokenPreference.resetAsync(context)
                                    urlToken = TempData.urlToken
                                    DialogHelper.showMessage(R.string.the_token_is_reset)
                                }
                            },
                        )
                        BottomSpace(paddingValues)
                    }
                }
            },
        )
    }
}

@Composable
fun PasswordTextField(
    value: String = "",
    isChanged: () -> Boolean,
    onValueChange: (String) -> Unit = {},
    onConfirm: (String) -> Unit = {},
) {
    Box(modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 0.dp)) {
        val clipboardManager = LocalClipboardManager.current
        val focusRequester = remember { FocusRequester() }
        TextField(
            modifier =
                Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                ),
            maxLines = 1,
            value = value,
            onValueChange = {
                onValueChange(it)
            },
            visualTransformation = VisualTransformation.None,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.password),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            singleLine = true,
            trailingIcon = {
                if (value.isNotEmpty()) {
                    if (isChanged()) {
                        Button(
                            onClick = {
                                onConfirm(value)
                            },
                        ) {
                            Text(text = stringResource(id = R.string.save))
                        }
                    }
                } else {
                    IconButton(onClick = {
                        onValueChange(clipboardManager.getText()?.text ?: "")
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.content_paste),
                            contentDescription = stringResource(R.string.paste),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
        )
    }
}
