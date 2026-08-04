package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.arrow_left
import com.ismartcoding.plain.i18n.ellipsis
import com.ismartcoding.plain.i18n.more_info
import com.ismartcoding.plain.i18n.unknown
import com.ismartcoding.plain.lib.extensions.formatMinSec
import com.ismartcoding.plain.ui.base.PlayerSlider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Shared full-screen video overlay scaffold: top title bar (with exit + more
 * buttons) + center spacer + bottom progress slider + [VideoControlsBar].
 *
 * Reused by the MediaPreviewer fullscreen overlay (Android) and the DLNA
 * receiver video player. Caller supplies the [VideoControlsBar] via
 * [controls] so it can bind whichever state object (VideoState or engine)
 * it uses.
 *
 * @param title media title shown in the top bar (empty -> "Unknown")
 * @param positionMs current playback position for the slider + label
 * @param durationMs total duration for the slider + label
 * @param onExit exit button click
 * @param onMore info button click (FileInfoBottomSheet); null hides the button.
 *   Ignored when [trailing] is non-null.
 * @param trailing optional custom trailing action rendered in the top bar
 *   (e.g. a download button). When non-null it replaces the info button.
 * @param controls the bottom control bar (Speed/Sound/Play/Pip/Fullscreen)
 */
@Composable
fun VideoOverlayScaffold(
    title: String,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    onExit: () -> Unit,
    onMore: (() -> Unit)?,
    trailing: @Composable (() -> Unit)? = null,
    controls: @Composable () -> Unit,
) {
    val darkOverlay = Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.7f),
        0.35f to Color.Black.copy(alpha = 0f),
        0.65f to Color.Black.copy(alpha = 0f),
        1f to Color.Black.copy(alpha = 0.7f),
    )
    Box(modifier = Modifier.fillMaxSize().background(darkOverlay)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp),
                ) {
                    IconButton(onClick = onExit, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_left),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                        Text(
                            text = title.ifEmpty { stringResource(Res.string.unknown) },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (trailing != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) { trailing() }
                    }
                } else if (onMore != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp),
                    ) {
                        IconButton(onClick = onMore, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                painter = painterResource(Res.drawable.ellipsis),
                                contentDescription = stringResource(Res.string.more_info),
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
            // Center spacer
            Box(modifier = Modifier.weight(1f))
            // Bottom controls
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Time row above slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = positionMs.formatMinSec(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = durationMs.formatMinSec(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                    )
                }
                // Progress slider
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                        PlayerSlider(
                            modifier = Modifier.fillMaxWidth().height(24.dp),
                            progress = progress,
                            bufferedProgress = 0f,
                            onProgressChange = onSeek,
                        )
                    }
                }
                // Control bar (Speed/Sound/Play/Pip/Fullscreen)
                controls()
            }
        }
    }
}
