package com.ismartcoding.plain.ui.page.web

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.filledButtonContent
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.launch

/**
 * Compose-drawn "video" demo for the Stay Online FAQ, mirroring the real
 * home screen and StayOnlineModeOverlay rendering: tap Stay Online on the
 * service card → keep-running overlay → go dark (screen turns fully black
 * while PlainApp keeps running).
 * Timeline: 0–2000ms home screen (tap ring on Stay Online),
 * 2000–4400ms overlay (tap ring on Go dark now), 4400–5600ms pure black.
 * Click the player to play/pause; tap or drag the progress bar to scrub.
 */

private const val DEMO_TOTAL_MS = 5600f
private const val DEMO_STEP1_MS = 2000f // home screen → overlay
private const val DEMO_STEP2_MS = 4400f // overlay → black
private val DemoChromeBg = Color(0xFF101114)
private val DemoTrack = Color(0xFF3A3B41)
private val DemoPillShape = RoundedCornerShape(50)

@Composable
fun FaqStayOnlineDemo() {
    var playing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun launchAnim() {
        val remainMs = ((1f - progress.value) * DEMO_TOTAL_MS).toInt().coerceAtLeast(16)
        scope.launch {
            progress.animateTo(1f, tween(remainMs, easing = LinearEasing))
            playing = false
        }
    }

    fun pausePlayback() {
        // Interrupts the running animateTo; its coroutine dies before touching `playing`.
        scope.launch { progress.stop() }
        playing = false
    }

    fun seekTo(fraction: Float) {
        scope.launch { progress.snapTo(fraction.coerceIn(0f, 1f)) }
    }

    fun resumeAfterSeek() {
        if (!playing) return
        if (progress.value >= 1f) playing = false else launchAnim()
    }

    fun playPause() {
        if (playing) {
            pausePlayback()
        } else {
            playing = true
            if (progress.value >= 1f) {
                scope.launch {
                    progress.snapTo(0f)
                    launchAnim()
                }
            } else {
                launchAnim()
            }
        }
    }

    val progressValue = progress.value
    val elapsedMs = progressValue * DEMO_TOTAL_MS
    val step = when {
        elapsedMs < DEMO_STEP1_MS -> 0
        elapsedMs < DEMO_STEP2_MS -> 1
        else -> 2
    }
    // Poster frame before the first play: same home screen, without the tap ring.
    val idle = !playing && progressValue <= 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DemoChromeBg)
            .clickable { playPause() },
    ) {
        // Phone screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (step >= 1) Color.Black else MaterialTheme.colorScheme.background)
                .heightIn(min = 232.dp),
            contentAlignment = if (step >= 1) Alignment.Center else Alignment.TopCenter,
        ) {
            if (step >= 1) {
                DemoStayOnlineOverlay(ring = step == 1)
            } else {
                DemoHomeScreen(ring = step == 0 && !idle)
            }
            if (!playing) {
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
        // Playback progress: tap to seek, drag to scrub
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
                .height(20.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        seekTo(offset.x / size.width.toFloat())
                        resumeAfterSeek()
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            seekTo(change.position.x / size.width.toFloat())
                        },
                        onDragEnd = { resumeAfterSeek() },
                        onDragCancel = { resumeAfterSeek() },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(DemoTrack),
            )
            Box(
                Modifier
                    .fillMaxWidth(progressValue.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.blue),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Miniature of the real home screen: service card (buttons on top) + Desktop Access card. */
@Composable
private fun DemoHomeScreen(ring: Boolean) {
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
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
                    text = "http://192.168.1.20",
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

@Composable
private fun DemoSwitchOn() {
    Box(
        modifier = Modifier
            .width(30.dp)
            .height(18.dp)
            .clip(DemoPillShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 2.dp)
                .size(14.dp)
                .background(Color.White, CircleShape),
        )
    }
}
