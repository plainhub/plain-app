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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.placeholder
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.PGlideImage
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun ImageGridItem(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: ImagesViewModel,
    m: DImage,
) {
    val isSelected = viewModel.selectedIds.contains(m.id) || viewModel.selectedItem.value?.id == m.id
    val selectedSize by animateDpAsState(
        if (isSelected) 12.dp else 0.dp, label = "selectedSize"
    )
    Box(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    if (viewModel.selectMode.value) {
                        viewModel.select(m.id)
                    } else {
                        PreviewDialog().show(
                            items = viewModel.itemsFlow.value.map { s -> PreviewItem(s.id, s.path.pathToUri(), s.path) },
                            initKey = m.id,
                        )
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
        PGlideImage(
            model = m.path,
            contentDescription = m.path,
            modifier = if (isSelected) imageModifier
                .padding(selectedSize)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                ) else imageModifier,
            contentScale = ContentScale.Crop,
            transition = CrossFade,
            failure = placeholder(R.drawable.ic_broken_image),
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