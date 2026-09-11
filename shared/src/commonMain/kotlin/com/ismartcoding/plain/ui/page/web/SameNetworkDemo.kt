package com.ismartcoding.plain.ui.page.web

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.green

/**
 * Compose-drawn "video" for Quick start step 1, replaying the real flow:
 * click the Wi-Fi icon in the macOS menu bar → the popover lists known
 * networks → pick the same network on the phone's Wi-Fi list too → compare
 * the two IPs, the shared network prefix flashing on both ("same network").
 * Steps: 0 menu bar + tap ring on the Wi-Fi icon, 1 popover + ring on
 * Home_5G, 2 phone Wi-Fi list connecting to the same network,
 * 3 Mac/phone IP comparison with the flashing prefix.
 * The phone IP is the device's real IP; the Mac IP reuses its prefix.
 */

private const val DEMO_WIFI_NAME = "Home"
private const val DEMO_WIFI_NAME_5G = "Home_5G"
private const val DEMO_PC_IP_SUFFIX = "100"

@Composable
fun SameNetworkDemo() {
    val phoneIp = demoServerIp()
    val prefix = phoneIp.substringBeforeLast(".") + "."
    val macIp = prefix + DEMO_PC_IP_SUFFIX

    DemoPlayer(
        stepDurationsMs = listOf(1200L, 1600L, 1800L),
        endHoldMs = 1400L,
        contentMinHeight = 248.dp,
        contentBackground = MaterialTheme.colorScheme.background,
    ) { step, _ ->
        when {
            step == 0 -> MacMenuBar(ring = true)
            step == 1 -> Column(modifier = Modifier.fillMaxWidth()) {
                MacMenuBar(ring = false)
                MacWifiPopover(ring = true)
            }
            step == 2 -> PhoneWifiList()
            else -> IpCompare(prefix = prefix, pcIp = macIp, phoneIp = phoneIp)
        }
    }
}

/** macOS menu bar mock: faint system icons on the right, Wi-Fi highlighted as clicked. */
@Composable
private fun MacMenuBar(ring: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
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
            if (ring) DemoTapRing()
        }
    }
}

/** macOS Wi-Fi popover: toggle, known networks, the 5G one selected. */
@Composable
private fun MacWifiPopover(ring: Boolean) {
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
            Box(contentAlignment = Alignment.CenterStart) {
                WifiMenuRow(name = DEMO_WIFI_NAME_5G, selected = true)
                if (ring) DemoTapRing()
            }
        }
    }
}

/** Phone Wi-Fi settings mock: same list, Home_5G connected. */
@Composable
private fun PhoneWifiList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
            Box(contentAlignment = Alignment.CenterStart) {
                Column {
                    WifiMenuRow(name = DEMO_WIFI_NAME_5G, selected = true)
                    Spacer(Modifier.height(2.dp))
                    Row {
                        Spacer(Modifier.width(38.dp))
                        Text(
                            text = stringResource(Res.string.faq_https_demo_connected),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.green,
                        )
                    }
                }
                DemoTapRing()
            }
        }
    }
}

@Composable
private fun WifiMenuRow(name: String, selected: Boolean) {
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

/** Split view: Mac network details (left) vs the phone's IP (right), shared prefix flashing. */
@Composable
private fun IpCompare(prefix: String, pcIp: String, phoneIp: String) {
    val transition = rememberInfiniteTransition(label = "demoPrefixFlash")
    val flash by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "flash",
    )
    val prefixBg = MaterialTheme.colorScheme.blue.copy(alpha = 0.35f * flash)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IpCard(
                modifier = Modifier.weight(1f),
                icon = Res.drawable.laptop,
                rows = listOf("IP address" to pcIp, "Router" to (prefix + "1")),
                flashPrefix = prefix,
                prefixBg = prefixBg,
            )
            IpCard(
                modifier = Modifier.weight(1f),
                icon = Res.drawable.smartphone,
                rows = listOf("IP address" to phoneIp),
                flashPrefix = prefix,
                prefixBg = prefixBg,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.check),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.green,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(Res.string.demo_same_network),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IpCard(
    modifier: Modifier,
    icon: DrawableResource,
    rows: List<Pair<String, String>>,
    flashPrefix: String,
    prefixBg: Color,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.cardBackgroundNormal)
            .padding(12.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IpValue(value = value, flashPrefix = flashPrefix, prefixBg = prefixBg)
            }
        }
    }
}

@Composable
private fun IpValue(value: String, flashPrefix: String, prefixBg: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (value.startsWith(flashPrefix)) {
            Text(
                text = flashPrefix,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.background(prefixBg),
            )
            Text(
                text = value.removePrefix(flashPrefix),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
