package com.ismartcoding.plain.ui.sessions

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.features.sessions.RustWebLogin
import com.ismartcoding.plain.features.sessions.WebLoginRequestFacts
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.capitalize
import com.ismartcoding.plain.ui.components.RequestDecisionActions
import com.ismartcoding.plain.ui.components.RequestPageLayout
import com.ismartcoding.plain.ui.components.RequestVerificationCodeCard
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.lock as ui_drawable_lock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginRequestPage(
    loginRequest: WebLoginRequestFacts,
    onClose: () -> Unit,
) {
    PBackHandler(onBack = onClose)
    val request = loginRequest.request
    val descRes = if (request.peer != null) Res.string.allow_remote_chat_access_desc else Res.string.allow_remote_access_desc
    val device = listOf(
        request.browserName.capitalize().trim(),
        request.osName.capitalize().trim(),
    ).filter { it.isNotEmpty() }.joinToString(" · ")
    val requester = listOf(device, loginRequest.clientIp).filter { it.isNotEmpty() }.joinToString("   ")

    RequestPageLayout(
        title = stringResource(Res.string.home_remote_access),
        description = stringResource(descRes),
        icon = painterResource(UiRes.drawable.ui_drawable_lock),
        requester = requester,
        content = {
            if (request.verificationCode.isNotEmpty()) {
                item {
                    RequestVerificationCodeCard(request.verificationCode, stringResource(Res.string.login_compare_code))
                }
            }
        },
        actions = {
            RequestDecisionActions(
                onDeny = { RustWebLogin.cancel(loginRequest.requestId); onClose() },
                onAllow = { RustWebLogin.complete(loginRequest.requestId); onClose() },
            )
        },
    )
}
