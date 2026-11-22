package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.models.MediaBucket
import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.ui.media.MediaThumbnailGrid
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import com.ismartcoding.plain.i18n.folder

@Composable
fun MediaSidebarBucketItem(
    m: MediaBucket,
    isMedia: Boolean = false,
    isSelected: Boolean = false,
    drawerOpen: Boolean,
    onClick: () -> Unit,
) {
    val thumbnailPaths = sidebarBucketThumbnailPaths(m, isMedia, drawerOpen)
    val showThumbnail = thumbnailPaths.isNotEmpty()
    SidebarItem(
        label = m.name,
        subtitle = m.size.formatBytes(),
        isSelected = isSelected,
        badge = m.itemCount.toString(),
        icon = if (showThumbnail) null else UiRes.drawable.ui_drawable_folder,
        leading = if (showThumbnail) {
            {
                MediaThumbnailGrid(
                    paths = thumbnailPaths,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.medium),
                )
            }
        } else null,
        onClick = onClick,
    )
}

internal fun sidebarBucketThumbnailPaths(bucket: MediaBucket, isMedia: Boolean, drawerOpen: Boolean): List<String> =
    if (isMedia && drawerOpen) bucket.topItemPaths else emptyList()
