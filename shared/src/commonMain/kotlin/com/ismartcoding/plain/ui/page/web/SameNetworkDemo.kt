package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.red

/**
 * Compose-drawn "video" for Quick start step 1, replaying the real flow:
 * click the Wi-Fi icon in the macOS menu bar → the popover lists known
 * networks → the phone joins the same Wi-Fi ("Same Wi-Fi").
 * Steps: 0 "Computer" menu bar + tap ring on the Wi-Fi icon,
 * 1 "Computer" popover + ring on Home_5G,
 * 2 "Phone · Same Wi-Fi" list connecting to the same network.
 */

private const val DEMO_WIFI_NAME = "Home"
private const val DEMO_WIFI_NAME_5G = "Home_5G"

@Composable
fun SameNetworkDemo() {
    DemoPlayer(
        stepDurationsMs = listOf(1200L, 1600L),
        endHoldMs = 1400L,
        contentMinHeight = 248.dp,
        contentBackground = MaterialTheme.colorScheme.background,
    ) { step, _ ->
        when {
            step == 0 -> DemoSceneLabelled("Computer") { MacMenuBar(highlight = true) }
            step == 1 -> DemoSceneLabelled("Computer") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MacMenuBar(highlight = false)
                    MacWifiPopover()
                }
            }
            else -> DemoSceneLabelled("Phone · " + stringResource(Res.string.demo_same_wifi)) {
                PhoneWifiList()
            }
        }
    }
}

/** Red rounded outline marking the element the user "taps" next (replaces the tap ring). */
private fun Modifier.demoTapOutline(enabled: Boolean, color: Color): Modifier =
    if (enabled) this.border(2.dp, color, RoundedCornerShape(10.dp)) else this

/** Small grey caption naming the device shown in the scene below. */
@Composable
private fun DemoSceneLabelled(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        )
        content()
    }
}

/** macOS menu bar mock: faint system icons on the right, Wi-Fi highlighted as clicked. */
@Composable
private fun MacMenuBar(highlight: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
            .heightIn(min = 30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.cardBackgroundNormal)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(width = 18.dp, height = 12.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(12.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f), CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .demoTapOutline(highlight, MaterialTheme.colorScheme.red)
                    .padding(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.blue)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.wifi),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

/** macOS Wi-Fi popover: toggle, known networks, the 5G one selected. */
@Composable
private fun MacWifiPopover() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.cardBackgroundNormal)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Wi-Fi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                DemoSwitchOn()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Known Networks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            WifiMenuRow(name = DEMO_WIFI_NAME, selected = false)
            Spacer(Modifier.height(6.dp))
            WifiMenuRow(name = DEMO_WIFI_NAME_5G, selected = true, highlighted = true)
        }
    }
}

/** Phone Wi-Fi settings mock: same list, Home_5G connected. */
@Composable
private fun PhoneWifiList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.cardBackgroundNormal)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Wi-Fi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                DemoSwitchOn()
            }
            Spacer(Modifier.height(12.dp))
            WifiMenuRow(name = DEMO_WIFI_NAME, selected = false)
            Spacer(Modifier.height(10.dp))
            WifiMenuRow(name = DEMO_WIFI_NAME_5G, selected = true, highlighted = true)
            Spacer(Modifier.height(2.dp))
            Row {
                Spacer(Modifier.width(44.dp))
                Text(
                    text = stringResource(Res.string.faq_https_demo_connected),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.green,
                )
            }
        }
    }
}

@Composable
private fun WifiMenuRow(name: String, selected: Boolean, highlighted: Boolean = false) {
    Box(
        modifier = Modifier
            .demoTapOutline(highlighted, MaterialTheme.colorScheme.red)
            .padding(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.blue
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.wifi),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(Res.drawable.lock),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        }
    }
}
