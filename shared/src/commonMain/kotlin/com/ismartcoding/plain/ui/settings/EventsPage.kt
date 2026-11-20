package com.ismartcoding.plain.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.*
import com.ismartcoding.plain.platform.system.formatDate
import com.ismartcoding.plain.platform.system.formatTime
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTextField
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.close as ui_drawable_close
import com.ismartcoding.plain.ui.resources.code as ui_drawable_code
import com.ismartcoding.plain.ui.resources.search as ui_drawable_search
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val EventTimeWidth = 64.dp
private val EventNodeWidth = 8.dp
private val EventColumnGap = 8.dp
private val EventContentStart = EventTimeWidth + EventNodeWidth + EventColumnGap * 2
private val EventHeaderHeight = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsPage(navController: NavHostController, model: EventsViewModel) {
    val developerMode by Prefs.developerMode.collectAsState()
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    val closeSearch = {
        searchVisible = false
        if (model.search.isNotEmpty()) model.search("")
    }
    val scope = rememberCoroutineScope()
    val refreshState = rememberRefreshLayoutState {
        val request = model.refresh()
        scope.launch {
            request.join()
            setRefreshState(if (model.error.isEmpty()) RefreshContentState.Finished else RefreshContentState.Failed)
        }
    }
    LaunchedEffect(model) { model.refresh() }
    PBackHandler(enabled = searchVisible, onBack = closeSearch)

    PScaffold(topBar = {
        PTopAppBar(
            title = stringResource(Res.string.events),
            onNavigateBack = { if (searchVisible) closeSearch() else navController.navigateUp() },
            actions = {
                if (searchVisible) {
                    PIconButton(UiRes.drawable.ui_drawable_close, contentDescription = stringResource(Res.string.close), click = closeSearch)
                } else {
                    PIconButton(UiRes.drawable.ui_drawable_search, contentDescription = stringResource(Res.string.search)) {
                        searchVisible = true
                    }
                }
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (searchVisible) {
                PTextField(
                    readOnly = false,
                    value = model.search,
                    label = stringResource(Res.string.search),
                    onValueChange = model::search,
                    requestFocus = true,
                    showPasteAction = false,
                    modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN, vertical = 8.dp),
                )
            }
            PullToRefresh(
                modifier = Modifier.weight(1f),
                refreshLayoutState = refreshState,
                userEnable = !model.loading,
            ) {
                val entries = model.entries
                val dates = remember(entries) { entries.map { it.createdAt.formatDate() } }
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                        val startsDay = dates.getOrNull(index - 1) != dates[index]
                        if (startsDay) {
                            Text(
                                text = dates[index],
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = EventContentStart, top = 16.dp).semantics { heading() },
                            )
                        }
                        EventTimelineRow(entry, !startsDay, dates.getOrNull(index + 1) == dates[index], developerMode)
                    }
                    if (model.error.isNotEmpty()) item {
                        Text(model.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 16.dp))
                    }
                    if (!model.loading && entries.isEmpty() && model.error.isEmpty()) item {
                        Text(
                            text = stringResource(Res.string.events_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (model.loading && refreshState.getRefreshContentState().value != RefreshContentState.Refreshing) item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    if (model.hasMore) item {
                        Box(Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                            PTextButton(text = stringResource(Res.string.events_load_more), onClick = model::loadMore, enabled = !model.loading)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventTimelineRow(entry: EventRecord, hasPrevious: Boolean, hasNext: Boolean, developerMode: Boolean) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val failed = entry.type in setOf(EventType.LOGIN_FAILED, EventType.MOUNT_FAILED, EventType.FORMAT_DISK_FAILED)
    val detail = eventMessage(entry.data)
    val facts = listOf(
        listOfNotNull(entry.appName, entry.appVersion).filter(String::isNotBlank).joinToString(" "),
        listOfNotNull(entry.osName, entry.osVersion).filter(String::isNotBlank).joinToString(" "),
        entry.clientIp.orEmpty(),
    ).filter(String::isNotBlank)
    Row(
        horizontalArrangement = Arrangement.spacedBy(EventColumnGap),
        modifier = Modifier.fillMaxWidth().drawBehind {
            val start = (EventTimeWidth + EventColumnGap + EventNodeWidth / 2).toPx()
            val x = if (layoutDirection == LayoutDirection.Ltr) start else size.width - start
            val y = (EventHeaderHeight / 2).toPx()
            if (hasPrevious || hasNext) {
                drawLine(
                    colors.outlineVariant,
                    start = Offset(x, if (hasPrevious) 0f else y),
                    end = Offset(x, if (hasNext) size.height else y),
                    strokeWidth = 4.dp.toPx(),
                )
            }
            drawCircle(if (failed) colors.error else colors.onSurfaceVariant, radius = (EventNodeWidth / 2).toPx(), center = Offset(x, y))
        }.padding(bottom = 16.dp),
    ) {
        Box(Modifier.width(EventTimeWidth).heightIn(min = EventHeaderHeight), contentAlignment = Alignment.CenterEnd) {
            Text(entry.createdAt.formatTime(), style = typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.End)
        }
        Box(Modifier.width(EventNodeWidth))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = EventHeaderHeight), verticalAlignment = Alignment.CenterVertically) {
                Text(eventTitle(entry.type), modifier = Modifier.weight(1f), style = typography.titleMedium)
                if (developerMode) EventRawData(entry)
            }
            entry.clientName?.takeIf(String::isNotBlank)?.let {
                Text(it, style = typography.labelLarge)
            }
            if (facts.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    facts.forEach { Text(it, style = typography.bodyMedium, color = colors.onSurfaceVariant) }
                }
            }
            if (detail.isNotEmpty()) {
                Text(detail, style = typography.bodyMedium, color = if (failed) colors.error else colors.onSurface)
            }
        }
    }
}

@Composable
private fun EventRawData(entry: EventRecord) {
    var open by remember { mutableStateOf(false) }
    Box {
        PIconButton(
            icon = UiRes.drawable.ui_drawable_code,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = stringResource(Res.string.event_view_raw),
        ) { open = true }
        if (open) {
            val json = remember(entry) { JsonHelper.jsonEncode(entry, pretty = true) }
            PDropdownMenu(expanded = true, onDismissRequest = { open = false }) {
                SelectionContainer(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = json,
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
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
    AuthErrorCode.TIMEOUT.wire -> Res.string.event_reason_login_expired
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
