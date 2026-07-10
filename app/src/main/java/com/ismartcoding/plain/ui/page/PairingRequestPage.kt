package com.ismartcoding.plain.ui.page

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.discover.PairingInitiator
import com.ismartcoding.plain.discover.PairingResponder
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.delay

private const val PAIRING_REQUEST_TIMEOUT_SECONDS = 90

@Composable
fun PairingRequestPage(
    request: DPairingRequest,
    navController: NavHostController,
) {
    var remainingSeconds by remember { mutableIntStateOf(PAIRING_REQUEST_TIMEOUT_SECONDS) }
    var expired by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
        expired = true
    }

    val displayIp = request.fromIp.ifEmpty { request.ips.firstOrNull() ?: "" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 48.dp),
    ) {
        item {
            VerticalSpace(40.dp)
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(request.deviceType.getIcon()),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            VerticalSpace(dp = 24.dp)
        }
        item {
            Text(
                text = stringResource(
                    if (expired) Res.string.pairing_request_expired
                    else Res.string.pairing_request
                ),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            VerticalSpace(dp = 16.dp)
            Text(
                text = stringResource(Res.string.pairing_request_message, request.fromName),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            VerticalSpace(dp = 40.dp)
        }
        item {
            PCard {
                PListItem(title = stringResource(Res.string.device_name), value = request.fromName)
                if (displayIp.isNotEmpty()) {
                    PListItem(title = stringResource(Res.string.ip_address), value = displayIp)
                }
                if (request.bleMac.isNotEmpty()) {
                    PListItem(title = stringResource(Res.string.mac_address), value = request.bleMac)
                }
            }
        }
        item {
            VerticalSpace(dp = 56.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                if (expired) {
                    PFilledButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.close),
                        buttonSize = ButtonSize.EXTRA_LARGE,
                        onClick = { navController.popBackStack() },
                    )
                } else {
                    Text(
                        text = "${remainingSeconds}s",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    )
                    PFilledButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.allow),
                        buttonSize = ButtonSize.EXTRA_LARGE,
                        onClick = {
                            coIO { PairingResponder.respond(request, true) }
                            navController.popBackStack()
                        },
                    )
                    VerticalSpace(24.dp)
                    PFilledButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.deny),
                        buttonSize = ButtonSize.EXTRA_LARGE,
                        type = ButtonType.DANGER,
                        onClick = {
                            coIO { PairingResponder.respond(request, false) }
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}
