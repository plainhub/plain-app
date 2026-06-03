package com.ismartcoding.plain.ui.page

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.VerticalSpace

@Composable
fun PairingRequestPage(
    event: PairingRequestReceivedEvent,
    onDeny: () -> Unit,
    onAllow: () -> Unit,
) {
    val request = event.request
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
                text = stringResource(Res.string.pairing_request),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            VerticalSpace(dp = 8.dp)
            Text(
                text = stringResource(Res.string.pairing_request_message, request.fromName),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            VerticalSpace(dp = 40.dp)
        }
        item {
            PCard {
                PListItem(title = stringResource(Res.string.device_name), value = request.fromName)
                PListItem(title = stringResource(Res.string.ip_address), value = request.fromIp)
            }
        }
        item {
            VerticalSpace(dp = 40.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                PFilledButton(
                    text = stringResource(Res.string.allow),
                    buttonSize = ButtonSize.LARGE,
                    onClick = onAllow,
                )
                VerticalSpace(24.dp)
                PFilledButton(
                    text = stringResource(Res.string.deny),
                    buttonSize = ButtonSize.LARGE,
                    onClick = onDeny,
                    type = ButtonType.DANGER,
                )
            }
        }
    }
}
