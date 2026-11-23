package com.ismartcoding.plain.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.ismartcoding.plain.features.chat.peer.ResourceSize
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.models.MediaBucketCover
import androidx.compose.runtime.collectAsState
import com.ismartcoding.plain.platform.media.applyForceVideoDecoder
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.extensions.rememberImageResource
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async

@Composable
fun MediaThumbnailGrid(
    items: List<MediaBucketCover>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val images = remember(items) { items.take(9) }
    if (images.isEmpty()) return
    val imageVersions = remember(images) { images.map { it.path to it.updatedAt } }
    val source = LocalContentSource.current
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
        val cellPixels = with(LocalDensity.current) { cellSize.roundToPx().coerceAtLeast(1) }
        val resourceSize = if (cellPixels <= 128) ResourceSize.XS else ResourceSize.SM
        val remoteThumbnails = if (source is ContentSource.Remote) {
            val info = source.client.currentInfo
            key(source, imageVersions, resourceSize, info.urlToken, info.tlsCertificateSha256) {
                val scope = rememberCoroutineScope()
                remember {
                    val resources = scope.async(start = CoroutineStart.LAZY) {
                        try {
                            Result.success(source.client.folderThumbnailResources(images, resourceSize))
                        } catch (canceled: CancellationException) {
                            throw canceled
                        } catch (error: Exception) {
                            Result.failure(error)
                        }
                    }
                    images.associate { cover ->
                        cover.path to source.client.versionedThumbnail(cover.path, resourceSize, cover.updatedAt) {
                            resources.await().getOrThrow().getValue(cover.path)
                        }
                    }
                }
            }
        } else {
            emptyMap()
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for (row in rows) {
                Row(horizontalArrangement = Arrangement.Center) {
                    for (cover in row) {
                        val path = cover.path
                        val data = if (source is ContentSource.Remote) {
                            remoteThumbnails[path]
                        } else {
                            rememberImageResource(path, source)
                        }
                        val context = LocalPlatformContext.current
                        val request = remember(context, source, path, data, cellPixels) {
                            ImageRequest.Builder(context)
                                .data(data)
                                .size(cellPixels)
                                .applyForceVideoDecoder(source == ContentSource.Local && path.isVideoFast())
                                .build()
                        }
                        val painter = rememberAsyncImagePainter(request)
                        val imageState by painter.state.collectAsState()
                        Box(Modifier.size(cellSize)) {
                            if (imageState is AsyncImagePainter.State.Success) {
                                Image(painter, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                ThumbnailPlaceholder(cover.thumbHash, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}
