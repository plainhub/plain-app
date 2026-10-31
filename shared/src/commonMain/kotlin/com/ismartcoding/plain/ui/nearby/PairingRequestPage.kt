package com.ismartcoding.plain.ui.nearby

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.features.nearby.PairingResponder
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.PairingRequestNotice
import com.ismartcoding.plain.ui.components.RequestDecisionActions
import com.ismartcoding.plain.ui.components.RequestPageLayout
import com.ismartcoding.plain.ui.components.RequestVerificationCodeCard
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.platform.system.getIcon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PairingRequestPage(
    notice: PairingRequestNotice,
    onClose: () -> Unit,
) {
    val request = notice.requestInput()
    PBackHandler(onBack = onClose)

    val displayIp = request.fromIp.ifEmpty { request.ips.firstOrNull() ?: "" }
    val requester = listOf(request.fromName, displayIp).filter { it.isNotEmpty() }.joinToString(" · ")

    RequestPageLayout(
        title = stringResource(Res.string.pairing_request),
        description = stringResource(Res.string.pairing_request_hint),
        icon = painterResource(request.deviceType.getIcon()),
        requester = requester,
        content = { item { RequestVerificationCodeCard(notice.verificationCode, stringResource(Res.string.pairing_compare_code)) } },
        actions = {
            RequestDecisionActions(
                onDeny = { PairingResponder.respond(request, false); onClose() },
                onAllow = { PairingResponder.respond(request, true); onClose() },
            )
        },
    )
}
