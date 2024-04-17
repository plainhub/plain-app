package com.ismartcoding.plain.ui.components

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.preference.HttpPortPreference
import com.ismartcoding.plain.preference.HttpsPortPreference
import com.ismartcoding.plain.features.WindowFocusChangedEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WebAddressBar(
    context: Context,
    isHttps: Boolean,
) {
    val port = if (isHttps) TempData.httpsPort else TempData.httpPort
    var portDialogVisible by remember { mutableStateOf(false) }
    var ip4 = remember { NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" } }
    var ip4s = remember { NetworkHelper.getDeviceIP4s().filter { it != ip4 } }
    val showContextMenu = remember { mutableStateOf(false) }
    val defaultUrl = remember { mutableStateOf("${if (isHttps) "https" else "http"}://$ip4:${port}") }
    val scope = rememberCoroutineScope()
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    LaunchedEffect(Unit) {
        events.add(
            receiveEventHandler<WindowFocusChangedEvent> {
                if (it.hasFocus) {
                    ip4 = NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" }
                    ip4s = NetworkHelper.getDeviceIP4s().filter { it != ip4 }
                    defaultUrl.value = "${if (isHttps) "https" else "http"}://$ip4:${port}"
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            events.forEach { it.cancel() }
            events.clear()
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionContainer {
            ClickableText(
                text = AnnotatedString(defaultUrl.value),
                modifier = Modifier.padding(start = 16.dp),
                style =
                TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                ),
                onClick = {
                    val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), defaultUrl.value)
                    clipboardManager.setPrimaryClip(clip)
                    DialogHelper.showTextCopiedMessage(defaultUrl.value)
                },
            )
        }
        PIconButton(
            icon = Icons.Rounded.Edit,
            modifier =
            Modifier
                .height(20.dp)
                .width(20.dp),
            contentDescription = stringResource(id = R.string.edit),
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = {
                portDialogVisible = true
            },
        )
        if (ip4s.isNotEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier =
                Modifier
                    .wrapContentSize(Alignment.TopEnd),
            ) {
                PIconButton(
                    icon = Icons.Rounded.MoreVert,
                    modifier =
                    Modifier
                        .height(20.dp)
                        .width(20.dp),
                    contentDescription = stringResource(id = R.string.more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        showContextMenu.value = true
                    },
                )
                PDropdownMenu(
                    expanded = showContextMenu.value,
                    onDismissRequest = { showContextMenu.value = false },
                ) {
                    ip4s.forEach { ip ->
                        val url = "${if (isHttps) "https" else "http"}://$ip:${port}"
                        PDropdownMenuItem(text = { Text(url) }, onClick = {
                            showContextMenu.value = false
                            val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), url)
                            clipboardManager.setPrimaryClip(clip)
                            DialogHelper.showTextCopiedMessage(url)
                        })
                    }
                }
            }
        }
    }

    if (portDialogVisible) {
        RadioDialog(
            title = stringResource(R.string.change_port),
            options =
            (if (isHttps) HttpServerManager.httpsPorts else HttpServerManager.httpPorts).map {
                RadioDialogOption(
                    text = it.toString(),
                    selected = it == port,
                ) {
                    scope.launch(Dispatchers.IO) {
                        if (isHttps) {
                            HttpsPortPreference.putAsync(context, it)
                        } else {
                            HttpPortPreference.putAsync(context, it)
                        }
                    }
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.restart_app_title)
                        .setMessage(R.string.restart_app_message)
                        .setPositiveButton(R.string.relaunch_app) { _, _ ->
                            AppHelper.relaunch(context)
                        }
                        .setCancelable(false)
                        .create()
                        .show()
                }
            },
        ) {
            portDialogVisible = false
        }
    }
}