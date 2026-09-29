package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.music2 as ui_drawable_music2

@Composable
fun PlaylistCoverArtwork(gradientIndex: Int, modifier: Modifier = Modifier, iconSize: Int = 28) {
    Box(
        modifier = modifier.background(audioHomeGradientAt(gradientIndex)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(UiRes.drawable.ui_drawable_music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}
