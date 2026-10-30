package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.models.Doc as Document
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.ismartcoding.plain.models.Doc
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isPdfFile
import com.ismartcoding.plain.lib.extensions.isTextFile
import com.ismartcoding.plain.platform.files.fileToUriString
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.ui.media.fileIconData
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.files.documents.DocsViewModel
import com.ismartcoding.plain.ui.navigation.navigateOtherFile
import com.ismartcoding.plain.ui.navigation.navigatePdf
import com.ismartcoding.plain.ui.navigation.navigateTextFile
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTag
import com.ismartcoding.plain.ui.theme.listItemTitle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocItem(
    navController: NavHostController,
    docsVM: DocsViewModel,
    dragSelectState: DragSelectState,
    m: Document,
    tags: List<Tag>,
    onTagClick: (Tag) -> Unit,
    onOpen: (() -> Unit)? = null,
) {
    Row {
        if (dragSelectState.selectMode) {
            HorizontalSpace(dp = 16.dp)
            CheckCircle(selected = dragSelectState.isSelected(m.id), onClick = { dragSelectState.select(m.id) })
        }
        Surface(
            modifier =
            PlainTheme
                .getCardModifier(selected = docsVM.selectedItem.value?.id == m.id || dragSelectState.isSelected(m.id))
                .combinedClickable(
                    onClick = {
                        if (dragSelectState.selectMode) {
                            dragSelectState.select(m.id)
                        } else if (onOpen != null) {
                            onOpen()
                        } else {
                            if (m.path.isTextFile()) {
                                navController.navigateTextFile(m.path, mediaId = m.id)
                            } else if (m.path.isPdfFile()) {
                                docsVM.prepareFile(m.path) { navController.navigatePdf(fileToUriString(it), m.title) }
                            } else {
                                navController.navigateOtherFile(m.path, m.title)
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
                    model =  fileIconData(m.title.getFilenameExtension()),
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
                                Text(
                                    text = "#" + tag.name,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clickable {
                                            if (!dragSelectState.selectMode) {
                                                onTagClick(tag)
                                            }
                                        },
                                    style = MaterialTheme.typography.listItemTag(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
