package com.ismartcoding.plain.ui.settings

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
import com.ismartcoding.plain.models.EventType
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
                    entry.createdAt.formatDateTime(), eventMessage(entry.type, entry.message),
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
    EventType.REVOKE -> Res.string.event_revoke
    EventType.SET_HOSTNAME -> Res.string.event_set_hostname
    EventType.UPDATE_DEVICE_NAME -> Res.string.event_update_device_name
    EventType.MOUNT -> Res.string.event_mount
    EventType.MOUNT_FAILED -> Res.string.event_mount_failed
    EventType.UNMOUNT -> Res.string.event_unmount
    EventType.FORMAT_DISK -> Res.string.event_format_disk
    EventType.FORMAT_DISK_FAILED -> Res.string.event_format_disk_failed
})

@Composable
private fun eventMessage(type: EventType, message: String): String {
    if (type != EventType.LOGIN_FAILED) return message
    val resource = when (message) {
        "invalid_password" -> Res.string.event_reason_invalid_password
        "invalid_login_request" -> Res.string.event_reason_invalid_login_request
        "remote_access_disabled" -> Res.string.event_reason_remote_access_disabled
        "login_expired" -> Res.string.event_reason_login_expired
        "too_many_login_attempts" -> Res.string.event_reason_too_many_login_attempts
        "rejected" -> Res.string.event_reason_rejected
        "cancelled" -> Res.string.event_reason_cancelled
        else -> return message
    }
    return stringResource(resource)
}
