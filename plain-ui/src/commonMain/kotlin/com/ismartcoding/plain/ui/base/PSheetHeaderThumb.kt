package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun PSheetHeaderThumb(
    model: Any?,
    fallbackIcon: DrawableResource,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier.size(44.dp).clip(MaterialTheme.shapes.extraSmall),
        contentScale = contentScale,
        error = painterResource(fallbackIcon),
        fallback = painterResource(fallbackIcon),
    )
}
