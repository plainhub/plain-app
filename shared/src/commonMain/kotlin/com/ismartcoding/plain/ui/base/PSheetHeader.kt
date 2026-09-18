package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// Standard 44dp sheet-header thumbnail: renders [model] with coil, falling
// back to [fallbackIcon] when the model is null or fails to load.
// Defaults to Fit so file icons never distort; pass Crop for photos.
@Composable
fun PSheetHeaderThumb(
    model: Any?,
    fallbackIcon: DrawableResource,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = Modifier.size(44.dp),
        contentScale = ContentScale.Crop,
        error = painterResource(fallbackIcon),
        fallback = painterResource(fallbackIcon),
    )
}

@Composable
fun PSheetHeader(
    thumbnail: @Composable () -> Unit,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        thumbnail()
        HorizontalSpace(16.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.listItemTitle(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                VerticalSpace(4.dp)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.listItemSubtitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
