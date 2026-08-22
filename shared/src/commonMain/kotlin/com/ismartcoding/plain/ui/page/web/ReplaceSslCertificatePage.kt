package com.ismartcoding.plain.ui.page.web

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.SslCertImportMode
import com.ismartcoding.plain.platform.queryPickedFileInfo
import com.ismartcoding.plain.platform.replaceSSLKeyStoreAsync
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplaceSslCertificatePage(navController: NavHostController) {
    var mode by remember { mutableStateOf(SslCertImportMode.PKCS12) }
    var certUri by remember { mutableStateOf("") }
    var certName by remember { mutableStateOf("") }
    var keyUri by remember { mutableStateOf("") }
    var keyName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val sharedFlow = Channel.sharedFlow
    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            if (event is PickFileResultEvent) {
                val uri = event.uris.firstOrNull() ?: return@collect
                when (event.tag) {
                    PickFileTag.SSL_CERT, PickFileTag.SSL_CERT_PEM -> {
                        certUri = uri
                        certName = queryPickedFileInfo(uri)?.displayName ?: uri
                    }

                    PickFileTag.SSL_KEY_PEM -> {
                        keyUri = uri
                        keyName = queryPickedFileInfo(uri)?.displayName ?: uri
                    }

                    else -> {}
                }
            }
        }
    }

    val ready = when (mode) {
        SslCertImportMode.PKCS12 -> certUri.isNotEmpty()
        SslCertImportMode.PEM -> certUri.isNotEmpty() && keyUri.isNotEmpty()
    }

    val onImport = {
        scope.launch(Dispatchers.Default) {
            importing = true
            DialogHelper.showLoading()
            try {
                replaceSSLKeyStoreAsync(
                    mode = mode,
                    firstUri = certUri,
                    secondUri = if (mode == SslCertImportMode.PEM) keyUri else "",
                    password = password,
                )
                DialogHelper.hideLoading()
                DialogHelper.showConfirmDialog("", LocaleHelper.getStringAsync(Res.string.ssl_certificate_replaced)) {
                    sendEvent(RestartAppEvent())
                }
            } catch (ex: Exception) {
                LogCat.e("Replace SSL certificate failed: ${ex.message}")
                DialogHelper.hideLoading()
                DialogHelper.showErrorMessage(ex.message ?: LocaleHelper.getStringAsync(Res.string.error))
            } finally {
                importing = false
            }
        }
    }

    PScaffold(
        topBar = { PTopAppBar(navController = navController, title = stringResource(Res.string.https_certificate)) },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item { TopSpace() }
                item {
                    PCard {
                        VerticalSpace(dp = 16.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            PSelectionChip(
                                selected = mode == SslCertImportMode.PKCS12,
                                onClick = { mode = SslCertImportMode.PKCS12 },
                                text = stringResource(Res.string.pkcs12),
                                modifier = Modifier.weight(1f),
                            )
                            PSelectionChip(
                                selected = mode == SslCertImportMode.PEM,
                                onClick = { mode = SslCertImportMode.PEM },
                                text = stringResource(Res.string.pem),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Tips(
                            text = stringResource(
                                if (mode == SslCertImportMode.PKCS12) Res.string.pkcs12_mode_desc
                                else Res.string.pem_mode_desc
                            )
                        )
                        VerticalSpace(dp = 8.dp)
                        if (mode == SslCertImportMode.PKCS12) {
                            FilePickerRow(
                                label = stringResource(Res.string.certificate_file),
                                fileName = certName,
                                onPick = { sendEvent(PickFileEvent(PickFileTag.SSL_CERT, PickFileType.FILE, false)) },
                            )
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                PTextField(
                                    readOnly = false,
                                    value = password,
                                    label = stringResource(Res.string.certificate_password),
                                    isPassword = true,
                                    onValueChange = { password = it },
                                )
                            }
                        } else {
                            FilePickerRow(
                                label = stringResource(Res.string.certificate_file),
                                fileName = certName,
                                onPick = { sendEvent(PickFileEvent(PickFileTag.SSL_CERT_PEM, PickFileType.FILE, false)) },
                            )
                            FilePickerRow(
                                label = stringResource(Res.string.private_key_file),
                                fileName = keyName,
                                onPick = { sendEvent(PickFileEvent(PickFileTag.SSL_KEY_PEM, PickFileType.FILE, false)) },
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    VerticalSpace(dp = 16.dp)
                    PFilledButton(
                        modifier = Modifier
                            .fillMaxWidth().padding(horizontal = 16.dp),
                        text = stringResource(Res.string.import_certificate),
                        isLoading = importing,
                        enabled = ready && !importing,
                        onClick = { onImport() },
                    )
                    Tips(text = stringResource(Res.string.import_ssl_certificate_tips))
                    BottomSpace(paddingValues)
                }
            }
        })
}

@Composable
private fun FilePickerRow(
    label: String,
    fileName: String,
    onPick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSpace(dp = 8.dp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = fileName.ifEmpty { stringResource(Res.string.no_file_selected) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (fileName.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            HorizontalSpace(8.dp)
            POutlinedButton(
                text = stringResource(Res.string.select),
                buttonSize = ButtonSize.MEDIUM,
                onClick = onPick,
            )
        }
        VerticalSpace(dp = 12.dp)
    }
}
