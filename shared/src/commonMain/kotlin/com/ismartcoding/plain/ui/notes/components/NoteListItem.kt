package com.ismartcoding.plain.ui.notes.components

import com.ismartcoding.plain.ui.components.CheckCircle

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.extensions.timeAgo
import com.ismartcoding.plain.models.Note
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.state.select
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemDescription
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTag

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteListItem(
    m: Note,
    tags: List<Tag>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    notesVM: NotesViewModel = NotesViewModel.local,
    onClickTag: (Tag) -> Unit
) {
    Row {
        if (notesVM.selectMode.value) {
            HorizontalSpace(dp = 16.dp)
            CheckCircle(selected = notesVM.selectedIds.contains(m.id), onClick = { notesVM.select(m.id) })
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
                    text = m.title,
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
                        Text(
                            text = "#" + tag.name,
                            modifier = Modifier
                                .wrapContentHeight()
                                .align(Alignment.Bottom)
                                .clickable { onClickTag(tag) },
                            style = MaterialTheme.typography.listItemTag(),
                        )
                    }
                }
            }
        }
    }
}
