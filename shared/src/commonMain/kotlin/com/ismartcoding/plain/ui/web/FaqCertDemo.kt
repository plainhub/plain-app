package com.ismartcoding.plain.ui.web

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.lib.UrlHelper
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_right as ui_drawable_chevron_right
import com.ismartcoding.plain.ui.resources.lock as ui_drawable_lock
import com.ismartcoding.plain.ui.resources.triangle_alert as ui_drawable_triangle_alert


@Composable
fun FaqCertDemo(serverIp: String) {
    val port = Prefs.httpsPort.collectAsState()
    val host = UrlHelper.buildUrl("https", serverIp, port.value).removePrefix("https://")

    DemoPlayer(
        stepDurationsMs = listOf(1600L, 2400L),
        endHoldMs = 800L,
        contentMinHeight = 280.dp,
        contentBackground = DemoChromeBg,
    ) { step, elapsedMs ->
        DemoBrowserFrame(
            url = "https://" + host,
            icon = if (step >= 2) UiRes.drawable.ui_drawable_lock else UiRes.drawable.ui_drawable_triangle_alert,
            iconTint = if (step >= 2) MaterialTheme.colorScheme.green else MaterialTheme.colorScheme.error,
        ) {
            if (step >= 2) {
                DemoLoginPage()
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

@Composable
private fun DemoErrorPage(host: String, expanded: Boolean, ringOnAdvanced: Boolean, ringOnProceed: Boolean) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Icon(
            painter = painterResource(UiRes.drawable.ui_drawable_triangle_alert),
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
        Box(
            modifier = Modifier.demoTapRing(ringOnAdvanced),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.faq_https_demo_advanced),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(UiRes.drawable.ui_drawable_chevron_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = if (expanded) 90f else 0f },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (expanded) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.demoTapRing(ringOnProceed),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(Res.string.faq_https_demo_proceed, host),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

@Composable
internal fun Modifier.demoTapRing(enabled: Boolean): Modifier {
    if (!enabled) return this
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
    val color = MaterialTheme.colorScheme.primary
    return this.drawWithContent {
        drawContent()
        drawCircle(
            color = color,
            radius = 22.dp.toPx() * scale,
            style = Stroke(width = 2.dp.toPx()),
            alpha = alpha,
        )
    }
}
