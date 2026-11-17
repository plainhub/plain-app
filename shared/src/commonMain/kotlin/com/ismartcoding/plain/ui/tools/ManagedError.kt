package com.ismartcoding.plain.ui.tools

import com.ismartcoding.plain.models.ErrorCode
import com.ismartcoding.plain.models.BusinessException
import com.ismartcoding.plain.rust.graphql.GraphqlHttpException
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ManagedError(error: Throwable, modifier: Modifier = Modifier) {
    val code = (error as? BusinessException)?.code
    val description = when {
        code == ErrorCode.CONNECTION_TIMEOUT -> stringResource(Res.string.feed_err_timeout)
        code in setOf(ErrorCode.UNAUTHORIZED, ErrorCode.PEER_IDENTITY_CHANGED, ErrorCode.PEER_REMOVED) -> stringResource(Res.string.managed_authorization_changed)
        code in setOf(ErrorCode.NO_PERMISSION, ErrorCode.REMOTE_ACCESS_DISABLED) -> stringResource(Res.string.managed_permission_denied)
        error is GraphqlHttpException && error.status in 500..599 -> stringResource(Res.string.feed_err_server)
        else -> error.message ?: error.toString()
    }
    PAlert(description = description, type = AlertType.WARNING, modifier = modifier)
}
