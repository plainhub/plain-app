package com.ismartcoding.plain.ui.nearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.nearby.PairingResponder
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.PairingRequestNotice
import com.ismartcoding.plain.ui.base.CircularTimer
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.components.RequestDecisionActions
import com.ismartcoding.plain.ui.components.RequestPageLayout
import com.ismartcoding.plain.ui.components.RequestVerificationCodeCard
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.platform.system.getIcon
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val PAIRING_REQUEST_TIMEOUT_SECONDS = 90

@Composable
fun PairingRequestPage(
    notice: PairingRequestNotice,
    onClose: () -> Unit,
) {
    val request = notice.requestInput()
    var remainingSeconds by remember { mutableIntStateOf(PAIRING_REQUEST_TIMEOUT_SECONDS) }
    val expired = remainingSeconds == 0
    PBackHandler(onBack = onClose)

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    val displayIp = request.fromIp.ifEmpty { request.ips.firstOrNull() ?: "" }
    val requester = listOf(request.fromName, displayIp).filter { it.isNotEmpty() }.joinToString(" · ")

    RequestPageLayout(
        title = stringResource(if (expired) Res.string.pairing_request_expired else Res.string.pairing_request),
        description = stringResource(Res.string.pairing_request_hint),
        icon = painterResource(request.deviceType.getIcon()),
        requester = requester,
        content = { item { RequestVerificationCodeCard(notice.verificationCode, stringResource(Res.string.pairing_compare_code)) } },
        footer = if (expired) null else ({ PairingExpiry(remainingSeconds) }),
        actions = {
            if (expired) {
                PFilledButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.close),
                    buttonSize = ButtonSize.LARGE,
                    onClick = onClose,
                )
            } else {
                RequestDecisionActions(
                    onDeny = { PairingResponder.respond(request, false); onClose() },
                    onAllow = { PairingResponder.respond(request, true); onClose() },
                )
            }
        },
    )
}

@Composable
private fun PairingExpiry(remainingSeconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularTimer(
            progress = remainingSeconds.toFloat() / PAIRING_REQUEST_TIMEOUT_SECONDS,
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            trackColor = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "${(remainingSeconds / 60).toString().padStart(2, '0')}:${(remainingSeconds % 60).toString().padStart(2, '0')}",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.pairing_expiry_label),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
