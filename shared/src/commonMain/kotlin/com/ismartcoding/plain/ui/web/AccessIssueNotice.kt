package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.permissions.AccessAction
import com.ismartcoding.plain.platform.system.openAppSettings
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.components.ButtonSize
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AccessIssueNotice(item: AccessItem, enabled: Boolean, denied: Boolean, busy: Boolean, onRequest: () -> Unit) {
    item.issues.forEach { issue ->
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val description = when {
                issue.action == AccessAction.REQUEST && enabled -> Res.string.access_permission_unavailable
                denied -> Res.string.access_permission_denied
                else -> issue.description
            }
            Text(
                stringResource(description), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = if (denied || enabled && issue.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (issue.action == AccessAction.SETTINGS) {
                POutlinedButton(stringResource(Res.string.access_go_to_settings), onClick = { openAppSettings() }, buttonSize = ButtonSize.SMALL)
            } else if (issue.action == AccessAction.FOLDER || issue.action == AccessAction.REQUEST) {
                POutlinedButton(
                    stringResource(checkNotNull(issue.actionText)), onClick = onRequest,
                    buttonSize = ButtonSize.SMALL, enabled = !busy,
                )
            }
        }
    }
}
