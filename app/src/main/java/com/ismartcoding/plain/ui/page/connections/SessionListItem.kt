package com.ismartcoding.plain.ui.page.connections

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.capitalize
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.VSession
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.web.HttpServerManager

@Composable
internal fun SessionListItem(
    m: VSession,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onHowToUse: () -> Unit,
) {
    val isOnline = HttpServerManager.wsSessions.any { it.clientId == m.clientId }
    val osDisplay = (m.osName.capitalize() + " " + m.osVersion).trim()
    val browserDisplay = (m.browserName.capitalize() + " " + m.browserVersion).trim()
    var showFullTime by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val lastActiveText = if (showFullTime) m.lastActiveAt?.formatDateTime() else m.lastActiveAt?.timeAgo()
    val displayName = m.name.ifEmpty { stringResource(Res.string.unknown) }
    val title = if (m.isCustom) displayName else osDisplay

    if (showRenameDialog) {
        TextFieldDialog(
            title = stringResource(Res.string.rename),
            value = m.name.ifEmpty { osDisplay },
            placeholder = stringResource(Res.string.name),
            confirmText = stringResource(Res.string.save),
            onDismissRequest = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(m.clientId, newName.trim())
                showRenameDialog = false
            },
        )
    }

    PCard {
        Column {
            SessionMainListItem(
                title = m.name.ifEmpty { osDisplay },
                subtitle = if (m.name.isEmpty()) "" else osDisplay,
                icon = if (m.isCustom) Res.drawable.lock else Res.drawable.laptop,
                onEditTitle = {
                    showRenameDialog = true
                },
                action = { SessionBadge(m = m, isOnline = isOnline) },
            )
            PListItem(
                title = stringResource(Res.string.client_id) + " - " + stringResource(Res.string.token),
                subtitle = m.clientId + "-" + m.token, action = {
                    Column(horizontalAlignment = Alignment.End) {
                        if (m.isCustom) {
                            PFilledButton(stringResource(Res.string.how_to_use), buttonSize = ButtonSize.SMALL, onClick = onHowToUse)
                        }
                        CopyIconButton(text = m.clientId + "-" + m.token, clipLabel = stringResource(Res.string.token))
                    }
                })
            if (m.clientIP.isNotEmpty()) {
                PListItem(title = stringResource(Res.string.ip_address), value = m.clientIP)
            }
            if (browserDisplay.isNotEmpty()) {
                PListItem(title = stringResource(Res.string.browser), value = browserDisplay)
            }
            PListItem(title = stringResource(Res.string.created_at), value = m.createdAt.formatDateTime())
            Text(
                text = stringResource(if (m.isCustom) Res.string.revoke_api_token else Res.string.revoke_session),
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
                text = stringResource(Res.string.last_active, lastActiveText),
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
