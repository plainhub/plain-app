package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ManagedError(message: String, modifier: Modifier = Modifier) {
    val description = when (message) {
        "connection_timeout" -> stringResource(Res.string.feed_err_timeout)
        "unauthorized", "peer_identity_changed", "peer_removed" -> stringResource(Res.string.managed_authorization_changed)
        "no_permission", "desktop_access_disabled" -> stringResource(Res.string.managed_permission_denied)
        else -> message
    }
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PAlert(description = description, type = AlertType.WARNING)
    }
}
