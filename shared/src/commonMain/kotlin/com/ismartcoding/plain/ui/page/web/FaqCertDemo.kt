package com.ismartcoding.plain.ui.page.web

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.green

/**
 * Compose-drawn "video" demo for the HTTPS certificate FAQ: the browser
 * warning page → Advanced → Proceed, ending on the loaded web page.
 * The address is built live from TempData (the real server IP + https port).
 * Steps: 0 error + tap ring on Advanced, 1 expanded + tap ring on Proceed,
 * 2 connected page. Played by the shared scrubbable DemoPlayer.
 */

@Composable
fun FaqCertDemo() {
    val port = TempData.httpsPort.collectAsState()
    val host = UrlHelper.buildUrl("https", demoServerIp(), port.value).removePrefix("https://")

    DemoPlayer(
        stepDurationsMs = listOf(1600L, 2400L),
        endHoldMs = 800L,
        contentMinHeight = 276.dp,
        contentBackground = DemoChromeBg,
    ) { step, elapsedMs ->
        Column(modifier = Modifier.fillMaxWidth()) {
            DemoBrowserChrome(
                url = "https://" + host,
                icon = if (step >= 2) Res.drawable.lock else Res.drawable.triangle_alert,
                iconTint = if (step >= 2) MaterialTheme.colorScheme.green else MaterialTheme.colorScheme.error,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .animateContentSize()
                        .heightIn(min = 208.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (step >= 2) {
                        DemoLoadedPage(minHeight = 208.dp)
                    } else {
                        DemoErrorPage(
                            host = host,
                            expanded = step >= 1,
                            ringOnAdvanced = step == 0 && elapsedMs > 0f,
                            ringOnProceed = step == 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoErrorPage(host: String, expanded: Boolean, ringOnAdvanced: Boolean, ringOnProceed: Boolean) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Icon(
            painter = painterResource(Res.drawable.triangle_alert),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.faq_https_demo_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.faq_https_demo_desc, host),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.faq_https_demo_reason, host),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.CenterStart) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.faq_https_demo_advanced),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(Res.drawable.chevron_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = if (expanded) 90f else 0f },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (ringOnAdvanced) DemoTapRing()
        }
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Box(contentAlignment = Alignment.CenterStart) {
                Text(
                    text = stringResource(Res.string.faq_https_demo_proceed, host),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.blue,
                    textDecoration = TextDecoration.Underline,
                )
                if (ringOnProceed) DemoTapRing()
            }
        }
    }
}

/** Pulsing circle drawn over the element being "tapped". */
@Composable
internal fun DemoTapRing() {
    val transition = rememberInfiniteTransition(label = "demoTapRing")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Restart),
        label = "scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Restart),
        label = "alpha",
    )
    Box(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .size(44.dp)
            .border(2.dp, MaterialTheme.colorScheme.blue, CircleShape),
    )
}
