package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.platform.permissions.AccessAction
import com.ismartcoding.plain.platform.permissions.requestSystemAccess
import com.ismartcoding.plain.platform.system.openAppSettings
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.state.launchSafe
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AccessIssueNotice(item: AccessItem) {
    val scope = rememberCoroutineScope()
    item.issues.forEach { issue ->
        Surface(
            color = if (issue.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (issue.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(issue.description), style = MaterialTheme.typography.bodyMedium)
                issue.actionText?.let { text ->
                    PTextButton(stringResource(text), onClick = {
                        when (issue.action) {
                            AccessAction.SETTINGS -> openAppSettings()
                            AccessAction.REQUEST, AccessAction.FOLDER -> scope.launchSafe { issue.features.requestSystemAccess() }
                            null -> Unit
                        }
                    })
                }
            }
        }
    }
}
