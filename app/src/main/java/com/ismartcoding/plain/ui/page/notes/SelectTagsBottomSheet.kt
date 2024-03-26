package com.ismartcoding.plain.ui.page.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PMiniButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSelectionChip
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.components.NewTagButton
import com.ismartcoding.plain.ui.components.TagNameDialog
import com.ismartcoding.plain.ui.models.TagsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectTagsBottomSheet(
    tagsViewModel: TagsViewModel,
    tagsState: List<DTag>,
    ids: Set<String>,
    removeFromTags: Boolean = false,
    onDismiss: () -> Unit,
) {
    TagNameDialog(tagsViewModel)
    val tagIds = remember { mutableStateListOf<String>() }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        PBottomSheetTopAppBar(stringResource(id = if (removeFromTags) R.string.remove_from_tags else R.string.add_to_tags), actions = {
            if (tagIds.isNotEmpty()) {
                PMiniButton(
                    text = stringResource(id = R.string.save),
                    onClick = {
                        if (removeFromTags) {
                            tagsViewModel.removeFromTags(ids, tagIds.toSet())
                        } else {
                            tagsViewModel.addToTags(ids, tagIds.toSet())
                        }
                        onDismiss()
                    },
                )
            }
        })
        TopSpace()
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
                        if (tagIds.contains(tag.id)) {
                            tagIds.remove(tag.id)
                        } else {
                            tagIds.add(tag.id)
                        }
                    },
                    text = tag.name
                )
            }
            NewTagButton(click = {
                tagsViewModel.showAddTagDialog()
            })
        }
        BottomSpace()
    }
}


