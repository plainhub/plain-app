package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.settings.PasswordType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.toSignature
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.*
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WebSecurityPage(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val passwordType = Prefs.passwordType.collectAsStateValue()
    val passwordConfigured = Prefs.passwordConfigured.collectAsStateValue()
    val loginRequireApproval = Prefs.loginRequireApproval.collectAsStateValue()
    var sslSignature by remember { mutableStateOf("") }
    val state = remember {
        PasswordSettingsState(
            readSettings = { RustNativeApi.passwordSettings() },
            writePassword = { Prefs.setPassword(it) },
            writeEnabled = { Prefs.setPasswordType(if (it) PasswordType.FIXED else PasswordType.NONE) },
            createPassword = { Prefs.generatePassword() },
        )
    }
    LaunchedEffect(passwordType, passwordConfigured) {
        if (!state.editing) state.refresh()
    }
    DisposableEffect(state) { onDispose { state.clearDraft() } }
    val cancelEdit = {
        if (!state.busy) {
            if (state.hasDraft) {
                DialogHelper.showConfirmDialog(
                    title = LocaleHelper.getString(Res.string.discard_changes),
                    message = LocaleHelper.getString(Res.string.password_discard_description),
                    confirmButton = Pair(LocaleHelper.getString(Res.string.discard_password_changes)) { state.discard() },
                    dismissButton = Pair(LocaleHelper.getString(Res.string.keep_editing)) {},
                )
            } else state.discard()
        }
    }
    PBackHandler(enabled = state.editing, onBack = cancelEdit)
    if (state.editing) {
        PasswordEditorPage(
            state = state, onCancel = cancelEdit,
            onSave = { scope.launch { state.save() } },
            onGenerate = { scope.launch { state.generate() } },
        )
        return
    }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            try {
                sslSignature = RustNativeApi.tlsCertificateSignature().toSignature()
            } catch (ex: Exception) {
                LogCat.e("Failed to get SSL signature: ${ex.message}")
            }
        }
    }
    PScaffold(
        topBar = { PTopAppBar(onNavigateBack = { navController.navigateUp() }, title = stringResource(Res.string.security)) },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            item { TopSpace() }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (state.saved) {
                        PAlert(stringResource(Res.string.password_save_succeeded), AlertType.INFO)
                    }
                    if (state.error == PasswordSettingsError.ENABLE) {
                        PAlert(stringResource(Res.string.password_enable_failed), AlertType.ERROR)
                    }
                    PasswordStatusCard(
                        state = state,
                        onEdit = { state.edit() },
                        onRetry = { scope.launch { state.refresh() } },
                        onToggle = { enabled ->
                            if (enabled) {
                                scope.launch { state.setEnabled(true) }
                            } else {
                                DialogHelper.showConfirmDialog(
                                    title = LocaleHelper.getString(Res.string.disable_password_login),
                                    message = LocaleHelper.getString(Res.string.disable_password_login_description),
                                    confirmButton = Pair(LocaleHelper.getString(Res.string.disable_password_login)) {
                                        scope.launch { state.setEnabled(false) }
                                    },
                                    dismissButton = Pair(LocaleHelper.getString(Res.string.cancel)) {},
                                )
                            }
                        },
                    )
                }
            }
            item {
                VerticalSpace(dp = 16.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(
                        modifier = Modifier.clickable { scope.launch(Dispatchers.Default) { Prefs.setLoginRequireApproval(!loginRequireApproval )} },
                        title = stringResource(Res.string.require_confirmation)
                    ) {
                        Switch(checked = loginRequireApproval, onCheckedChange = {
                            scope.launch(Dispatchers.Default) { Prefs.setLoginRequireApproval(it )}
                        })
                    }
                }
                Tips(text = stringResource(Res.string.two_factor_auth_tips)); VerticalSpace(dp = 24.dp)
            }
            item {
                PSectionTitle(title = stringResource(Res.string.https_certificate_signature))
                CornerCopyCard(
                    label = stringResource(Res.string.https_certificate_signature),
                    modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
                    text = sslSignature
                )
                VerticalSpace(dp = 16.dp)
                PFilledButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    text = stringResource(Res.string.reset_ssl_certificate),
                    type = ButtonType.DANGER, onClick = {
                        scope.launch(Dispatchers.Default) {
                            DialogHelper.showLoading()

                            sslSignature = RustNativeApi.regenerateTlsCertificate().toSignature()
                            HttpServerManager.restartHttpServer()
                            DialogHelper.hideLoading()
                            DialogHelper.showConfirmDialog("", LocaleHelper.getStringAsync(Res.string.ssl_certificate_reset))
                        }
                    })
                VerticalSpace(dp = 16.dp)
                PFilledButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    text = stringResource(Res.string.replace_ssl_certificate),
                    onClick = { navController.navigate(Routing.ReplaceSslCertificate) })
                BottomSpace(paddingValues)
            }
        }
    }
}
