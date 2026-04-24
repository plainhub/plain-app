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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
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
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.docs.DDoc
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.nav.navigateOtherFile
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.theme.listItemTag
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocItem(
    navController: NavHostController,
    docsVM: DocsViewModel,
    dragSelectState: DragSelectState,
    m: DDoc,
    tags: List<DTag>,
    onTagClick: (DTag) -> Unit,
) {
    Row {
        if (dragSelectState.selectMode) {
            HorizontalSpace(dp = 16.dp)
            Checkbox(checked = dragSelectState.isSelected(m.id), onCheckedChange = {
                dragSelectState.select(m.id)
            })
        }
        Surface(
            modifier =
            PlainTheme
                .getCardModifier(selected = docsVM.selectedItem.value?.id == m.id || dragSelectState.isSelected(m.id))
                .combinedClickable(
                    onClick = {
                        if (dragSelectState.selectMode) {
                            dragSelectState.select(m.id)
                        } else {
                            if (m.path.isTextFile()) {
                                navController.navigateTextFile(m.path, mediaId = m.id)
                            } else if (m.path.isPdfFile()) {
                                navController.navigatePdf(File(m.path).toUri())
                            } else {
                                navController.navigateOtherFile(m.path)
                            }
                        }
                    },
                    onLongClick = {
                        if (dragSelectState.selectMode) {
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
                    model =  AppHelper.getFileIconPath(m.title.getFilenameExtension()),
                    contentDescription = m.title,
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
                        text = m.title,
                        style = MaterialTheme.typography.listItemTitle(),
                    )
                    VerticalSpace(dp = 8.dp)
                    Text(
                        text = m.size.formatBytes() + ", " + m.updatedAt.formatDateTime(),
                        style = MaterialTheme.typography.listItemSubtitle(),
                    )
                    if (tags.isNotEmpty()) {
                        VerticalSpace(dp = 8.dp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            tags.forEach { tag ->
                                ClickableText(
                                    text = AnnotatedString("#" + tag.name),
                                    modifier = Modifier.padding(end = 8.dp),
                                    style = MaterialTheme.typography.listItemTag(),
                                    onClick = {
                                        if (!dragSelectState.selectMode) {
                                            onTagClick(tag)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}