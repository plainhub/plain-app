package com.ismartcoding.plain.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ManagedError(message: String, modifier: Modifier = Modifier) {
    val description = when {
        message == "connection_timeout" -> stringResource(Res.string.feed_err_timeout)
        message in setOf("unauthorized", "peer_identity_changed", "peer_removed") -> stringResource(Res.string.managed_authorization_changed)
        message in setOf("no_permission", "desktop_access_disabled") -> stringResource(Res.string.managed_permission_denied)
        message.startsWith("HTTP ") && message.removePrefix("HTTP ").toIntOrNull() in 500..599 -> stringResource(Res.string.feed_err_server)
        else -> message
    }
    PAlert(description = description, type = AlertType.WARNING, modifier = modifier)
}
