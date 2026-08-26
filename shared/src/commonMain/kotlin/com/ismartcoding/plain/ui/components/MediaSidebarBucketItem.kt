package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.platform.combineBitmapGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sidebar drawer item for a media bucket (folder). Shows a combined thumbnail
 * for media buckets or a folder icon otherwise, with the bucket size as subtitle.
 */
@Composable
fun MediaSidebarBucketItem(
    m: DMediaBucket,
    isMedia: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val bitmapResult = remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(m.id, m.topItems) {
        bitmapResult.value = withContext(Dispatchers.Default) {
            combineBitmapGrid(m.topItems, 150)
        }
    }

    val showThumbnail = isMedia && m.topItems.isNotEmpty()
    SidebarItem(
        label = m.name,
        subtitle = m.size.formatBytes(),
        isSelected = isSelected,
        badge = m.itemCount.toString(),
        icon = if (showThumbnail) null else Res.drawable.folder,
        leading = if (showThumbnail) {
            {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalPlatformContext.current)
                            .data(bitmapResult.value)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        } else null,
        onClick = onClick,
    )
}
