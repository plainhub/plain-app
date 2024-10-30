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
import com.ismartcoding.lib.extensions.getSummary
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemDescription
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTag

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteListItem(
    notesVM: NotesViewModel,
    m: DNote,
    tags: List<DTag>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickTag: (DTag) -> Unit
) {
    Row {
        if (notesVM.selectMode.value) {
            HorizontalSpace(dp = 16.dp)
            Checkbox(checked = notesVM.selectedIds.contains(m.id), onCheckedChange = {
                notesVM.select(m.id)
            })
        }

        Surface(
            modifier =
            PlainTheme
                .getCardModifier(selected = notesVM.selectedItem.value?.id == m.id || notesVM.selectedIds.contains(m.id))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .weight(1f),
            color = Color.Unspecified,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = m.title.getSummary(),
                    style = MaterialTheme.typography.listItemDescription(),
                )
                VerticalSpace(dp = 8.dp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = m.updatedAt.timeAgo(),
                        style = MaterialTheme.typography.listItemSubtitle(),
                    )
                    tags.forEach { tag ->
                        ClickableText(
                            text = AnnotatedString("#" + tag.name),
                            modifier = Modifier
                                .wrapContentHeight()
                                .align(Alignment.Bottom),
                            style = MaterialTheme.typography.listItemTag(),
                            onClick = {
                                onClickTag(tag)
                            }
                        )
                    }
                }
            }
        }
    }
}
