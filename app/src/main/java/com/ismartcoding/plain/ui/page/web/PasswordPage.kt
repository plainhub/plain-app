package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.data.preference.AuthTwoFactorPreference
import com.ismartcoding.plain.data.preference.LocalAuthTwoFactor
import com.ismartcoding.plain.data.preference.LocalPassword
import com.ismartcoding.plain.data.preference.LocalPasswordType
import com.ismartcoding.plain.data.preference.PasswordPreference
import com.ismartcoding.plain.data.preference.PasswordTypePreference
import com.ismartcoding.plain.data.preference.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BlockRadioButton
import com.ismartcoding.plain.ui.base.BlockRadioGroupButtonItem
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.DisplayText
import com.ismartcoding.plain.ui.base.OutlineButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordPage(navController: NavHostController) {
    WebSettingsProvider {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val passwordType = LocalPasswordType.current
        val password = LocalPassword.current
        val authTwoFactor = LocalAuthTwoFactor.current

        val editPassword = remember { mutableStateOf("") }
        LaunchedEffect(password) {
            editPassword.value = password
        }

        PScaffold(
            navController,
            content = {
                LazyColumn {
                    item {
                        DisplayText(text = stringResource(R.string.password_settings))
                        BlockRadioButton(
                            selected = passwordType,
                            onSelected = {
                                scope.launch {
                                    withIO { PasswordTypePreference.putAsync(context, PasswordType.parse(it)) }
                                }
                            },
                            itemRadioGroups =
                                PasswordType.values().map {
                                    BlockRadioGroupButtonItem(
                                        text = it.getText(),
                                        onClick = {},
                                    ) {
                                    }
                                },
                        )
                        if (passwordType == PasswordType.RANDOM.value) {
                            PListItem(
                                title = password,
                            ) {
                                OutlineButton(text = stringResource(id = R.string.reset), onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        PasswordPreference.putAsync(context, CryptoHelper.randomPassword(6))
                                    }
                                })
                            }
                        } else if (passwordType == PasswordType.FIXED.value) {
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
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        PListItem(
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
                        BottomSpace()
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
                        OutlineButton(
                            text = stringResource(id = R.string.confirm),
                            onClick = {
                                onConfirm(value)
                            },
                        )
                    }
                } else {
                    IconButton(onClick = {
                        onValueChange(clipboardManager.getText()?.text ?: "")
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
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
