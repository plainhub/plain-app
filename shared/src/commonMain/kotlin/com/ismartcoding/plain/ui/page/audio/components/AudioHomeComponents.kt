package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

/** Soft cover gradients rotated by a stable index (light theme, e-ink friendly). */
private val AudioHomeGradients = listOf(
    listOf(Color(0xFFDDE2F9), Color(0xFFE8DEF8)),
    listOf(Color(0xFFE5F0FF), Color(0xFFDDE2F9)),
    listOf(Color(0xFFE8DEF8), Color(0xFFF2E7EF)),
    listOf(Color(0xFFDFF0E4), Color(0xFFDDE2F9)),
    listOf(Color(0xFFFFF0DC), Color(0xFFE8DEF8)),
    listOf(Color(0xFFDDE2F9), Color(0xFFDFF0E4)),
)

fun audioHomeGradientAt(index: Int): Brush {
    val colors = AudioHomeGradients[abs(index) % AudioHomeGradients.size]
    return Brush.linearGradient(colors)
}

/** Stable gradient index for a path so covers keep their color across screens. */
fun audioHomeGradientIndexOf(path: String): Int = abs(path.hashCode()) % AudioHomeGradients.size

@Composable
fun PlaylistCoverArtwork(gradientIndex: Int, modifier: Modifier = Modifier, iconSize: Int = 28) {
    Box(
        modifier = modifier.background(audioHomeGradientAt(gradientIndex)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}

@Composable
fun ArtistAvatar(name: String, gradientIndex: Int, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(audioHomeGradientAt(gradientIndex)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "#",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
