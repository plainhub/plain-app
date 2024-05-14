package com.ismartcoding.plain.ui.components.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.base.mediaviewer.previewer.TransformImageView
import com.ismartcoding.plain.ui.base.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.VChat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatImages(
    context: Context,
    m: VChat,
    imageWidthDp: Dp,
    previewerState: MediaPreviewerState,
) {
    val imageItems = (m.value as DMessageImages).items

    FlowRow(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
        content = {
            imageItems.forEach { item ->
                val itemState = rememberTransformItemState()
                Box(
                    modifier =
                    Modifier.clickable {
                        coMain {
                            withIO { MediaPreviewData.setDataAsync(context, itemState, imageItems, item.id) }
                            previewerState.openTransform(
                                index = MediaPreviewData.items.indexOfFirst { it.id == item.id },
                                itemState = itemState,
                            )
                        }
                    },
                ) {
                    TransformImageView(
                        modifier = Modifier
                            .size(imageWidthDp)
                            .clip(RoundedCornerShape(6.dp)),
                        path = item.uri.getFinalPath(context),
                        key = item.id,
                        itemState = itemState,
                        previewerState = previewerState,
                    )
                    Box(
                        modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(bottomEnd = 6.dp))
                            .background(Color.Black.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            modifier =
                            Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            text =
                            if (item.duration > 0) {
                                FormatHelper.formatDuration(
                                    item.duration,
                                )
                            } else {
                                FormatHelper.formatBytes(item.size)
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        )
                    }
                }
            }
        },
    )
}
