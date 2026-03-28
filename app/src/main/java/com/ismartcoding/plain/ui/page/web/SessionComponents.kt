package com.ismartcoding.plain.ui.page.web

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.capitalize
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.VSession
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.web.HttpServerManager

@Composable
fun SubItem(
    @StringRes titleId: Int,
    value: String,
) {
    Text(
        text = stringResource(id = titleId),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    SelectionContainer {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
internal fun SessionListItem(
    m: VSession,
    onDelete: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.last_visit_at) + " " + m.updatedAt.formatDateTime(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        PIconButton(
            icon = R.drawable.delete_forever,
            contentDescription = stringResource(R.string.delete),
            tint = MaterialTheme.colorScheme.red,
            modifier = Modifier.padding(start = 8.dp),
            click = {
                DialogHelper.confirmToDelete {
                    onDelete(m.clientId)
                }
            }
        )
    }

    PCard {
        PListItem(title = stringResource(id = R.string.client_id), value = m.clientId)
        PListItem(title = stringResource(id = R.string.ip_address), value = m.clientIP)
        PListItem(title = stringResource(id = R.string.created_at), value = m.createdAt.formatDateTime())
        PListItem(title = stringResource(id = R.string.os), value = m.osName.capitalize() + " " + m.osVersion)
        PListItem(title = stringResource(id = R.string.browser), value = m.browserName.capitalize() + " " + m.browserVersion)
        PListItem(
            title = stringResource(id = R.string.status),
            value = stringResource(
                id = if (HttpServerManager.wsSessions.any { it.clientId == m.clientId }) R.string.online else R.string.offline,
            ),
        )
    }
    VerticalSpace(dp = 16.dp)
}
