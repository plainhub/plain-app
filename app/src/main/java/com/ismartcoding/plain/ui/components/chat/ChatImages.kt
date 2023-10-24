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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.size.Size
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.features.ChatItemClickEvent
import com.ismartcoding.plain.ui.base.PAsyncImage
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.models.SharedViewModel
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatImages(
    context: Context,
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
    m: VChat,
    imageWidthDp: Dp,
    imageWidthPx: Int,
) {
    FlowRow(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.Top),
        content = {
            val imageItems = (m.value as DMessageImages).items
            imageItems.forEachIndexed { index, item ->
                val path = item.uri.getFinalPath(context)
                Box(
                    modifier =
                    Modifier.clickable {
                        val items = imageItems.mapIndexed { i, s ->
                            val p = s.uri.getFinalPath(context)
                            PreviewItem(m.id + "|" + i, p.pathToUri(), p)
                        }
                        PreviewDialog().show(
                            items = items,
                            initKey = m.id + "|" + index,
                        )
//                        sharedViewModel.previewItems.value = items
//                        sharedViewModel.previewKey.value = m.id + "|" + index
//                        sharedViewModel.previewIndex.value = index
//                        navController.navigate(RouteName.MEDIA_PREVIEW)
                    },
                ) {
                    PAsyncImage(
                        modifier = Modifier.size(imageWidthDp),
                        data = path,
                        size = Size(imageWidthPx, imageWidthPx),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
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
