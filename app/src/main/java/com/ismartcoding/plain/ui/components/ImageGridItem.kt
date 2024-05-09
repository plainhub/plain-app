package com.ismartcoding.plain.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.mediaviewer.previewer.ImagePreviewerState
import com.ismartcoding.plain.ui.base.mediaviewer.previewer.TransformGlideImageView
import com.ismartcoding.plain.ui.base.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.select

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun ImageGridItem(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: ImagesViewModel,
    castViewModel: CastViewModel,
    m: DImage,
    previewerState: ImagePreviewerState,
) {
    val isSelected = viewModel.selectedIds.contains(m.id) || viewModel.selectedItem.value?.id == m.id
    val selectedSize by animateDpAsState(
        if (isSelected) 12.dp else 0.dp, label = "selectedSize"
    )
    val context = LocalContext.current
    val itemState = rememberTransformItemState()
    Box(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    if (castViewModel.castMode.value) {
                        castViewModel.cast(m.path)
                    } else if (viewModel.selectMode.value) {
                        viewModel.select(m.id)
                    } else {
                        coMain {
                            withIO { MediaPreviewData.setDataAsync(context, itemState, viewModel.itemsFlow.value, m) }
                            previewerState.openTransform(
                                index = MediaPreviewData.items.indexOfFirst { it.id == m.id },
                                itemState = itemState,
                            )
                        }
                    }
                },
                onLongClick = {
                    if (viewModel.selectMode.value) {
                        return@combinedClickable
                    }
                    viewModel.selectedItem.value = m
                },
            ),
    ) {
        val imageModifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center)
            .aspectRatio(1f)
        TransformGlideImageView(
            modifier = if (isSelected) imageModifier
                .padding(selectedSize)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                ) else imageModifier,
            path = m.path,
            key = m.id,
            itemState = itemState,
            previewerState = previewerState,
        )
        if (viewModel.selectMode.value) {
            Checkbox(
                modifier =
                Modifier
                    .align(Alignment.TopStart),
                checked = isSelected,
                onCheckedChange = {
                    viewModel.select(m.id)
                })
        }
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
                text = FormatHelper.formatBytes(m.size),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            )
        }
    }
}