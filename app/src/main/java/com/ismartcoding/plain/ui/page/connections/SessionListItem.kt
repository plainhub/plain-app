package com.ismartcoding.plain.ui.page.connections

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.capitalize
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.ClipboardCard
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.reorderable.draggable
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.VSession
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.web.HttpServerManager

@Composable
internal fun SessionListItem(
    m: VSession,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
) {
    val isOnline = HttpServerManager.wsSessions.any { it.clientId == m.clientId }
    val osDisplay = (m.osName.capitalize() + " " + m.osVersion).trim()
    val browserDisplay = (m.browserName.capitalize() + " " + m.browserVersion).trim()
    var showFullTime by remember { mutableStateOf(false) }
    var showTokenTipsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var inputName by remember(m.clientId, m.name) { mutableStateOf(m.name) }
    val lastActiveText = if (showFullTime) m.lastActiveAt?.formatDateTime() else m.lastActiveAt?.timeAgo()
    val displayName = m.name.ifEmpty { stringResource(R.string.unknown) }
    val title = if (m.isCustom) displayName else osDisplay

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(m.clientId, inputName.trim())
                    showRenameDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showTokenTipsDialog) {
        ApiTokenTipsDialog(m = m, onDismiss = { showTokenTipsDialog = false })
    }

    PCard {
        Column {
            SessionMainListItem(
                title = m.name.ifEmpty { osDisplay },
                subtitle = if (m.name.isEmpty()) "" else osDisplay,
                icon = if (m.isCustom) R.drawable.lock else R.drawable.laptop,
                onEditTitle = {
                    inputName = m.name
                    showRenameDialog = true
                },
                action = { SessionBadge(m = m, isOnline = isOnline) },
            )
            PListItem(
                title = stringResource(R.string.client_id) + " - " + stringResource(R.string.token),
                subtitle = m.clientId + "-" + m.token, action = {
                    Column(horizontalAlignment = Alignment.End) {
                        if (m.isCustom) {
                            PFilledButton(stringResource(R.string.how_to_use), buttonSize = ButtonSize.SMALL, onClick = {
                                showTokenTipsDialog = true
                            })
                        }
                        CopyIconButton(text = m.clientId + "-" + m.token, clipLabel = stringResource(R.string.token))
                    }
                })
            if (m.clientIP.isNotEmpty()) {
                PListItem(title = stringResource(R.string.ip_address), value = m.clientIP)
            }
            if (browserDisplay.isNotEmpty()) {
                PListItem(title = stringResource(R.string.browser), value = browserDisplay)
            }
            PListItem(title = stringResource(R.string.created_at), value = m.createdAt.formatDateTime())
            Text(
                text = stringResource(if (m.isCustom) R.string.revoke_api_token else R.string.revoke_session),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.red,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { DialogHelper.confirmToDelete { onDelete(m.clientId) } }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
    }

    if (lastActiveText != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.last_active, lastActiveText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showFullTime = !showFullTime }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
    VerticalSpace(dp = 16.dp)
}

@Composable
private fun ApiTokenTipsDialog(m: VSession, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val hostname = remember { TempData.mdnsHostname }
    val httpsPort = remember { TempData.httpsPort }

    val curlReal = remember(m, hostname, httpsPort) {
        """curl -X POST "https://$hostname:$httpsPort/graphql" -H "c-id: ${m.clientId}" -H "Authorization: Bearer ${m.token}" -H "Content-Type: application/json" --data '{"query":"{ app { version } }"}'"""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.api_tokens) + " · " + stringResource(R.string.how_to_use))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.auth_dev_token_tips),
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                )
                ClipboardCard(
                    horizontal = 0.dp,
                    label = "CURL",
                    text = curlReal,
                )
                PFilledButton(text = stringResource(id = R.string.docs), onClick = { WebHelper.open(context, "https://plainapp.app/api-docs") })
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

