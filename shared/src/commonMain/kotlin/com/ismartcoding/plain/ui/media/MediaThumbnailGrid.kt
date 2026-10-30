package com.ismartcoding.plain.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.platform.media.applyForceVideoDecoder
import com.ismartcoding.plain.ui.extensions.rememberImageResource

@Composable
fun MediaThumbnailGrid(
    paths: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val images = paths.take(9)
    if (images.isEmpty()) return
    val columns = when (images.size) {
        1 -> 1
        in 2..4 -> 2
        else -> 3
    }
    val firstRowSize = ((images.size - 1) % columns) + 1
    val rows = listOf(images.take(firstRowSize)) + images.drop(firstRowSize).chunked(columns)
    val semantics = if (contentDescription == null) Modifier else Modifier.semantics {
        this.contentDescription = contentDescription
    }
    BoxWithConstraints(modifier.then(semantics), contentAlignment = Alignment.Center) {
        val cellSize = minOf(maxWidth, maxHeight) / columns
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for (row in rows) {
                Row(horizontalArrangement = Arrangement.Center) {
                    for (path in row) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(rememberImageResource(path))
                                .applyForceVideoDecoder(path.isVideoFast())
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(cellSize),
                        )
                    }
                }
            }
        }
    }
}
