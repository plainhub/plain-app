package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.filledButtonContent
import com.ismartcoding.plain.ui.theme.red

/**
 * Compose-drawn "video" demo for the Stay Online FAQ, mirroring the real
 * home screen and StayOnlineModeOverlay rendering: tap Stay Online on the
 * service card → keep-running overlay → go dark (screen turns fully black
 * while PlainApp keeps running).
 * Steps: 0 home screen + tap ring on Stay Online, 1 overlay + tap ring on
 * Go dark now, 2 pure black screen. Played by the shared scrubbable DemoPlayer.
 */

private val DemoPillShape = RoundedCornerShape(50)

@Composable
fun FaqStayOnlineDemo() {
    val isHttps = TempData.webHttps.collectAsState()
    val port = if (isHttps.value) TempData.httpsPort.collectAsState() else TempData.httpPort.collectAsState()
    val url = remember(isHttps.value, port.value) {
        UrlHelper.buildUrl(if (isHttps.value) "https" else "http", demoServerIp(), port.value)
    }

    DemoPlayer(
        stepDurationsMs = listOf(2000L, 2400L),
        endHoldMs = 1200L,
        contentMinHeight = 232.dp,
        contentBackground = MaterialTheme.colorScheme.background,
    ) { step, elapsedMs ->
        Box(
            Modifier
                .matchParentSize()
                .background(if (step >= 1) Color.Black else Color.Transparent),
        )
        if (step >= 1) {
            Box(
                Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
            ) {
                DemoStayOnlineOverlay(ring = step == 1)
            }
        } else {
            Box(
                Modifier.matchParentSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                DemoHomeScreen(ring = step == 0 && elapsedMs > 0f, url = url)
            }
        }
    }
}

/** Miniature of the real home screen: service card (buttons on top) + Desktop Access card. */
@Composable
private fun DemoHomeScreen(ring: Boolean, url: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        // PlainAppServiceSection (ON state): title, then the equal-width button row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.plainapp_service_on),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DemoPillShape)
                            .border(1.dp, MaterialTheme.colorScheme.blue.copy(alpha = 0.5f), DemoPillShape)
                            .padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.stay_online),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.blue,
                        )
                    }
                    if (ring) DemoTapRing()
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DemoPillShape)
                            .background(MaterialTheme.colorScheme.red)
                            .padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.stop_service),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.filledButtonContent,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // DesktopAccessSection: blue devices icon + switch, tips, URL row, more-addresses hint
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.cardBackgroundNormal)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.devices),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.desktop_access),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                DemoSwitchOn()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.open_web_address),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(Res.drawable.pen),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(Res.drawable.qr_code),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.try_more_addresses),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(Res.drawable.expand_more),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The real StayOnlineModeOverlay: keep-running notice, Go dark now, tap to exit. */
@Composable
private fun DemoStayOnlineOverlay(ring: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.stay_online_keep_running),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .clip(DemoPillShape)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), DemoPillShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.stay_online_go_dark_now),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            if (ring) DemoTapRing()
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.stay_online_tap_to_exit),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}
