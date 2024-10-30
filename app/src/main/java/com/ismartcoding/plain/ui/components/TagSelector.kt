package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.ui.base.PSelectionChip
import com.ismartcoding.plain.ui.models.TagsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelector(
    data: IData,
    tagsVM: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
    onChanged: () -> Unit
) {
    val tagIds = remember {
        mutableStateListOf<String>()
    }
    LaunchedEffect(Unit) {
        tagIds.addAll(tagsMap[data.id]?.map { it.tagId } ?: emptyList())
    }
    TagNameDialog(tagsVM, onChanged)
    FlowRow(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        tagsState.forEach { tag ->
            PSelectionChip(
                selected = tagIds.contains(tag.id),
                onClick = {
                    tagsVM.toggleTag(data, tag.id)
                    if (tagIds.contains(tag.id)) {
                        tagIds.remove(tag.id)
                    } else {
                        tagIds.add(tag.id)
                    }
                    onChanged()
                },
                text = tag.name
            )
        }
        NewTagButton(click = {
            tagsVM.showAddDialog()
        })
    }
}