package com.ismartcoding.plain.ui.components

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil.size.Size
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PAsyncImage
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.navigateOtherFile
import com.ismartcoding.plain.ui.extensions.navigatePdf
import com.ismartcoding.plain.ui.extensions.navigateTextFile
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocItem(
    navController: NavHostController,
    viewModel: DocsViewModel,
    m: DFile,
) {
    val context = LocalContext.current
    Row {
        if (viewModel.selectMode.value) {
            HorizontalSpace(dp = 16.dp)
            Checkbox(checked = viewModel.selectedIds.contains(m.id), onCheckedChange = {
                viewModel.select(m.id)
            })
        }
        Surface(
            modifier =
            PlainTheme
                .getCardModifier(selected = viewModel.selectedItem.value?.id == m.id || viewModel.selectedIds.contains(m.id))
                .combinedClickable(
                    onClick = {
                        if (viewModel.selectMode.value) {
                            viewModel.select(m.id)
                        } else {
                            if (m.path.isTextFile()) {
                                navController.navigateTextFile(m.path, mediaStoreId = m.mediaStoreId)
                            } else if (m.path.isPdfFile()) {
                                navController.navigatePdf(File(m.path).toUri())
                            } else {
                                navController.navigateOtherFile(m.path)
                            }
                        }
                    },
                    onLongClick = {
                        if (viewModel.selectMode.value) {
                            return@combinedClickable
                        }
                        viewModel.selectedItem.value = m
                    },
                )
                .weight(1f),
            color = Color.Unspecified,
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp, 8.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                PAsyncImage(
                    contentDescription = m.name,
                    modifier = Modifier
                        .size(24.dp),
                    data = AppHelper.getFileIconPath(m.name.getFilenameExtension()),
                    size = Size(context.dp2px(24), context.dp2px(24)),
                )
                HorizontalSpace(dp = 16.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = m.name,
                        style = MaterialTheme.typography.listItemTitle(),
                    )
                    VerticalSpace(dp = 8.dp)
                    Text(
                        text = FormatHelper.formatBytes(m.size) + ", " + m.updatedAt.formatDateTime(),
                        style = MaterialTheme.typography.listItemSubtitle(),
                    )
                }
            }
        }
    }
}