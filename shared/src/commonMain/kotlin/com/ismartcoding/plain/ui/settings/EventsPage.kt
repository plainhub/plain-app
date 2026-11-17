package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.models.AuthErrorCode
import com.ismartcoding.plain.models.DiskFormatErrorCode
import com.ismartcoding.plain.models.MountErrorCode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.*
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTextField
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsPage(navController: NavHostController, model: EventsViewModel) {
    LaunchedEffect(model) { model.refresh() }
    PScaffold(topBar = {
        PTopAppBar(title = stringResource(Res.string.events), onNavigateBack = { navController.navigateUp() }, actions = {
            PTextButton(text = stringResource(Res.string.refresh), onClick = model::refresh, enabled = !model.loading)
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                PTextField(readOnly = false, value = model.search, label = stringResource(Res.string.search), onValueChange = model::search,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN, vertical = 8.dp))
            }
            if (model.error.isNotEmpty()) item {
                Text(model.error, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN, vertical = 8.dp))
            }
            if (!model.loading && model.entries.isEmpty() && model.error.isEmpty()) item {
                PListItem(title = stringResource(Res.string.events_empty))
            }
            items(model.entries, key = { it.id }) { entry ->
                PListItem(title = eventTitle(entry.type), subtitle = listOfNotNull(
                    entry.createdAt.formatDateTime(), eventMessage(entry.data),
                    entry.clientName,
                    listOfNotNull(entry.appName, entry.appVersion).joinToString(" "),
                    listOfNotNull(entry.osName, entry.osVersion).joinToString(" "), entry.clientIp,
                    entry.clientId?.let { "${stringResource(Res.string.event_client)}: $it" },
                ).filter { it.isNotEmpty() }.joinToString("\n"))
            }
            if (model.loading) item { CircularProgressIndicator(Modifier.padding(16.dp)) }
            if (model.hasMore) item {
                PTextButton(text = stringResource(Res.string.events_load_more), onClick = model::loadMore, enabled = !model.loading)
            }
        }
    }
}

@Composable
private fun eventTitle(type: EventType): String = stringResource(when (type) {
    EventType.LOGIN -> Res.string.event_login
    EventType.LOGIN_FAILED -> Res.string.event_login_failed
    EventType.LOGOUT -> Res.string.event_logout
    EventType.REVOKE_SESSION -> Res.string.event_revoke
    EventType.UPDATE_DEVICE_NAME -> Res.string.event_update_device_name
    EventType.MOUNT -> Res.string.event_mount
    EventType.MOUNT_FAILED -> Res.string.event_mount_failed
    EventType.UNMOUNT -> Res.string.event_unmount
    EventType.FORMAT_DISK -> Res.string.event_format_disk
    EventType.FORMAT_DISK_FAILED -> Res.string.event_format_disk_failed
})

@Composable
private fun eventMessage(data: EventData): String = when (data) {
    is LoginEventData -> ""
    is LoginFailedEventData -> eventReason(data.reason.wire)
    is SessionEventData -> data.session.clientName.ifEmpty { data.session.clientId }
    is DeviceNameEventData -> "${data.oldName} → ${data.newName}"
    is VolumeEventData -> listOfNotNull(data.volume.name, data.volume.mountPath).joinToString(" · ")
    is VolumeFailedEventData -> listOfNotNull(data.volume.name, data.volume.mountPath, eventReason(data.failure.reason.wire), data.failure.detail).joinToString(" · ")
    is DiskFormatEventData -> listOfNotNull(data.disk.name, data.disk.path, data.fileSystem).joinToString(" · ")
    is DiskFormatFailedEventData -> listOfNotNull(data.disk.name, data.disk.path, data.fileSystem, eventReason(data.failure.reason.wire), data.failure.detail).joinToString(" · ")
}

@Composable
private fun eventReason(reason: String): String = stringResource(when (reason) {
    AuthErrorCode.INVALID_PASSWORD.wire -> Res.string.event_reason_invalid_password
    AuthErrorCode.INVALID_LOGIN_REQUEST.wire -> Res.string.event_reason_invalid_login_request
    AuthErrorCode.REMOTE_ACCESS_DISABLED.wire -> Res.string.event_reason_remote_access_disabled
    AuthErrorCode.LOGIN_EXPIRED.wire -> Res.string.event_reason_login_expired
    AuthErrorCode.TOO_MANY_LOGIN_ATTEMPTS.wire -> Res.string.event_reason_too_many_login_attempts
    AuthErrorCode.REJECTED.wire -> Res.string.event_reason_rejected
    AuthErrorCode.CANCELED.wire -> Res.string.event_reason_canceled
    AuthErrorCode.PEER_IDENTITY_CHANGED.wire -> Res.string.event_reason_peer_identity_changed
    AuthErrorCode.PASSWORD_SETUP_REQUIRED.wire -> Res.string.event_reason_password_setup_required
    AuthErrorCode.INTERNAL_ERROR.wire -> Res.string.event_reason_internal_error
    MountErrorCode.MOUNT_FAILED.wire -> Res.string.event_mount_failed
    DiskFormatErrorCode.FORMAT_DISK_FAILED.wire -> Res.string.event_format_disk_failed
    else -> Res.string.event_reason_internal_error
})
