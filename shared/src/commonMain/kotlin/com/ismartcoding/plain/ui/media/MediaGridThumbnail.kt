package com.ismartcoding.plain.ui.media

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.time.Instant
import com.ismartcoding.plain.features.chat.peer.ResourceSize
import com.ismartcoding.plain.platform.system.ThumbnailNetworkSchedule
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState

@Composable
internal fun MediaGridThumbnail(
    uri: String,
    path: String,
    key: String,
    updatedAt: Instant,
    thumbHash: String?,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    networkSchedule: ThumbnailNetworkSchedule? = null,
) {
    BoxWithConstraints(Modifier.aspectRatio(1f)) {
        TransformImageViewWithUri(
            uri = uri,
            path = path,
            fileName = path.getFilenameFromPath(),
            key = key,
            version = updatedAt,
            itemState = itemState,
            previewerState = previewerState,
            widthPx = constraints.maxWidth,
            networkSchedule = networkSchedule,
            showPlaceholder = true,
            thumbHash = thumbHash,
            resourceSize = if (constraints.maxWidth in 1..128) ResourceSize.XS else ResourceSize.SM,
        )
    }
}
