package com.ismartcoding.plain.ui.page.web

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.isLanAddress
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.theme.blue
import kotlinx.coroutines.launch

/** First LAN IPv4 known to the app, or a stable fallback for the demos. */
internal fun demoServerIp(): String =
    TempData.ip4s.value.let { ips ->
        ips.firstOrNull { isLanAddress(it) && it.count { c -> c == '.' } == 3 }
            ?: ips.firstOrNull { it.count { c -> c == '.' } == 3 }
    } ?: "192.168.1.20"

/**
 * Shared player for the compose-drawn demo "videos": dark bezel, content box,
 * and a scrubbable playback bar. Frames are derived from playback progress, so
 * seeking always shows the frame at that point in time.
 *
 * @param stepDurationsMs duration of each step transition (size = stepCount - 1)
 * @param endHoldMs how long the final frame holds before playback stops
 */
@Composable
internal fun DemoPlayer(
    stepDurationsMs: List<Long>,
    endHoldMs: Long,
    contentMinHeight: Dp,
    contentBackground: Color,
    frame: @Composable BoxScope.(step: Int, elapsedMs: Float) -> Unit,
) {
    var playing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val boundaries = remember(stepDurationsMs) {
        buildList {
            var acc = 0f
            stepDurationsMs.forEach { acc += it; add(acc) }
        }
    }
    val totalMs = boundaries.last() + endHoldMs

    fun launchAnim() {
        val remainMs = ((1f - progress.value) * totalMs).toInt().coerceAtLeast(16)
        scope.launch {
            progress.animateTo(1f, tween(remainMs, easing = LinearEasing))
            playing = false
        }
    }

    fun pausePlayback() {
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
    val elapsedMs = progressValue * totalMs
    val step = boundaries.count { elapsedMs >= it }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DemoChromeBg)
            .clickable { playPause() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(contentBackground)
                .heightIn(min = contentMinHeight),
            contentAlignment = Alignment.TopCenter,
        ) {
            frame(step, elapsedMs)
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

internal val DemoChromeBg = Color(0xFF101114)
internal val DemoTrack = Color(0xFF3A3B41)
internal val DemoDot = Color(0xFF4A4B52)
internal val DemoUrlPill = Color(0xFF232429)
internal val DemoUrlText = Color(0xFFC9CAD1)
