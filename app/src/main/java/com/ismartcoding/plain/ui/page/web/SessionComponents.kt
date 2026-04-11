package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.capitalize
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.VSession
import com.ismartcoding.plain.ui.theme.greenDot
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.web.HttpServerManager

@Composable
internal fun SessionListItem(
    m: VSession,
    onDelete: (String) -> Unit,
) {
    val isOnline = HttpServerManager.wsSessions.any { it.clientId == m.clientId }
    val osDisplay = m.osName.capitalize() + " " + m.osVersion
    val browserDisplay = m.browserName.capitalize() + " " + m.browserVersion
    var showFullTime by remember { mutableStateOf(false) }
    val lastActiveText = if (showFullTime) m.updatedAt.formatDateTime() else m.updatedAt.timeAgo()

    PCard {
        Column {
            PListItem(
                title = osDisplay,
                subtitle = browserDisplay,
                icon = R.drawable.laptop,
                action = { StatusBadge(isOnline = isOnline) },
            )
            PListItem(title = stringResource(R.string.ip_address), value = m.clientIP)
            PListItem(title = stringResource(R.string.created_at), value = m.createdAt.formatDateTime())
            PListItem(title = stringResource(R.string.client_id), value = m.clientId)
            Text(
                text = stringResource(R.string.revoke_session),
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

    // Last active footer — tap to toggle between relative and full datetime
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
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

    VerticalSpace(dp = 12.dp)
}

@Composable
private fun StatusBadge(isOnline: Boolean) {
    val label = if (isOnline) stringResource(R.string.online) else stringResource(R.string.offline)
    val textColor = if (isOnline) Color.White else MaterialTheme.colorScheme.onSurface
    val bgColor = if (isOnline) MaterialTheme.colorScheme.greenDot else MaterialTheme.colorScheme.surfaceVariant
    Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
