package com.ismartcoding.plain.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.ManagedResource
import com.ismartcoding.plain.platform.media.applyForceVideoDecoder
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.extensions.rememberImageResource
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import kotlinx.coroutines.CancellationException

@Composable
fun MediaThumbnailGrid(
    paths: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val images = remember(paths) { paths.take(9) }
    if (images.isEmpty()) return
    val source = LocalContentSource.current
    val covers = if (source is ContentSource.Remote) {
        key(source, images) {
            val resources by produceState<Map<String, ManagedResource>>(emptyMap(), source, images) {
                try {
                    value = source.client.thumbnailResources(images)
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (error: Exception) {
                    LogCat.e(error.toString())
                }
            }
            resources
        }
    } else emptyMap()
    val columns = when (images.size) {
        1 -> 1
        in 2..4 -> 2
        else -> 3
    }
    val rows = remember(images, columns) {
        val firstRowSize = ((images.size - 1) % columns) + 1
        listOf(images.take(firstRowSize)) + images.drop(firstRowSize).chunked(columns)
    }
    val semantics = if (contentDescription == null) Modifier else Modifier.semantics {
        this.contentDescription = contentDescription
    }
    BoxWithConstraints(modifier.then(semantics), contentAlignment = Alignment.Center) {
        val cellSize = minOf(maxWidth, maxHeight) / columns
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for (row in rows) {
                Row(horizontalArrangement = Arrangement.Center) {
                    for (path in row) {
                        val data = if (source is ContentSource.Remote) covers[path] else rememberImageResource(path, source)
                        val context = LocalPlatformContext.current
                        val request = remember(context, source, path, data) {
                            ImageRequest.Builder(context)
                                .data(data)
                                .applyForceVideoDecoder(source == ContentSource.Local && path.isVideoFast())
                                .build()
                        }
                        AsyncImage(
                            model = request,
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
