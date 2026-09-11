package com.ismartcoding.plain.ui.page.web

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.green
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compose-drawn "video" demo for the HTTPS certificate FAQ: the browser
 * warning page → Advanced → Proceed, ending on the loaded web page.
 * Steps: -1 idle preview, 0 error + tap ring on Advanced,
 * 1 expanded + tap ring on Proceed, 2 connected page.
 */

private const val DEMO_HOST = "192.168.1.20"
private const val DEMO_TOTAL_MS = 4800f // step delays (1600 + 2400) + end hold 800
private val DemoChromeBg = Color(0xFF101114)
private val DemoChromeDim = Color(0xFF9A9BA3)
private val DemoTrack = Color(0xFF3A3B41)

@Composable
fun FaqCertDemo() {
    var step by remember { mutableIntStateOf(-1) }
    var playing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        val from = (step + 1).coerceIn(0, 2)
        if (from == 0) progress.snapTo(0f)
        launch {
            val remainMs = ((1f - progress.value) * DEMO_TOTAL_MS).toInt().coerceAtLeast(16)
            progress.animateTo(1f, tween(remainMs, easing = LinearEasing))
        }
        for (s in from..2) {
            step = s
            if (s == 2) break
            delay(if (s == 0) 1600L else 2400L)
        }
        delay(800L)
        playing = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DemoChromeBg)
            .clickable {
                if (playing) {
                    playing = false
                } else {
                    if (step >= 2) step = -1
                    playing = true
                }
            },
    ) {
        // Browser chrome: window dots + URL pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) {
                Box(
                    Modifier
                        .padding(end = 4.dp)
                        .size(8.dp)
                        .background(Color(0xFF4A4B52), CircleShape),
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF232429), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(if (step >= 2) Res.drawable.lock else Res.drawable.triangle_alert),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (step >= 2) MaterialTheme.colorScheme.green else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "https://" + DEMO_HOST,
                    color = Color(0xFFC9CAD1),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        // Page content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background)
                .animateContentSize()
                .heightIn(min = 208.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (step >= 2) {
                DemoLoadedPage()
            } else {
                DemoErrorPage(
                    expanded = step >= 1,
                    ringOnAdvanced = step == 0,
                    ringOnProceed = step == 1,
                )
            }
            if (!playing) {
                // matchParentSize: sized to this Box after wrap — fillMaxSize would
                // measure against the incoming (screen-height) constraints instead.
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.play_arrow),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
        // Playback progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(DemoTrack),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.blue),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DemoErrorPage(expanded: Boolean, ringOnAdvanced: Boolean, ringOnProceed: Boolean) {
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
            text = stringResource(Res.string.faq_https_demo_desc, DEMO_HOST),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.faq_https_demo_reason, DEMO_HOST),
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
                    text = stringResource(Res.string.faq_https_demo_proceed, DEMO_HOST),
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

@Composable
private fun DemoLoadedPage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 208.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            Icon(
                painter = painterResource(Res.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                tint = Color.Unspecified,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.green, CircleShape),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(Res.string.faq_https_demo_connected),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Pulsing circle drawn over the element being "tapped". */
@Composable
private fun DemoTapRing() {
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
