package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteListItem(
    viewModel: NotesViewModel,
    tagsViewModel: TagsViewModel,
    m: DNote,
    tags: List<DTag>,
    selectedItem: DNote?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row {
        if (viewModel.selectMode) {
            HorizontalSpace(dp = 16.dp)
            Checkbox(checked = viewModel.selectedIds.contains(m.id), onCheckedChange = {
                viewModel.select(m.id)
            })
        }

        Surface(
            modifier =
            PlainTheme
                .getCardModifier(selected = selectedItem?.id == m.id || viewModel.selectedIds.contains(m.id))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = m.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                    )
                    if (tags.isNotEmpty()) {
                        VerticalSpace(dp = 8.dp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tags.forEach { tag ->
                                ClickableText(
                                    text = AnnotatedString("#" + tag.name),
                                    modifier = Modifier
                                        .wrapContentHeight()
                                        .align(Alignment.Bottom),
                                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                                    onClick = {
                                        if (viewModel.selectMode) {
                                            return@ClickableText
                                        }
                                        viewModel.trash.value = false
                                        viewModel.tag.value = tag
                                        coIO {
                                            viewModel.loadAsync(tagsViewModel)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    VerticalSpace(dp = 8.dp)
                    Text(
                        text = m.updatedAt.timeAgo(),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary),
                    )
                }
            }
        }
    }
}
