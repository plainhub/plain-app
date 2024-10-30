package com.ismartcoding.plain.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.nav.navigateOtherFile
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
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
    docsVM: DocsViewModel,
    m: DFile,
) {
    Row {
        if (docsVM.selectMode.value) {
            HorizontalSpace(dp = 16.dp)
            Checkbox(checked = docsVM.selectedIds.contains(m.id), onCheckedChange = {
                docsVM.select(m.id)
            })
        }
        Surface(
            modifier =
            PlainTheme
                .getCardModifier(selected = docsVM.selectedItem.value?.id == m.id || docsVM.selectedIds.contains(m.id))
                .combinedClickable(
                    onClick = {
                        if (docsVM.selectMode.value) {
                            docsVM.select(m.id)
                        } else {
                            if (m.path.isTextFile()) {
                                navController.navigateTextFile(m.path, mediaId = m.mediaId)
                            } else if (m.path.isPdfFile()) {
                                navController.navigatePdf(File(m.path).toUri())
                            } else {
                                navController.navigateOtherFile(m.path)
                            }
                        }
                    },
                    onLongClick = {
                        if (docsVM.selectMode.value) {
                            return@combinedClickable
                        }
                        docsVM.selectedItem.value = m
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
                AsyncImage(
                    model =  AppHelper.getFileIconPath(m.name.getFilenameExtension()),
                    contentDescription = m.name,
                    modifier = Modifier
                        .size(24.dp),
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
                        text = m.size.formatBytes() + ", " + m.updatedAt.formatDateTime(),
                        style = MaterialTheme.typography.listItemSubtitle(),
                    )
                }
            }
        }
    }
}