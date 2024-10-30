package com.ismartcoding.plain.ui.page.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.ui.base.PSelectionChip
import com.ismartcoding.plain.ui.components.NewTagButton
import com.ismartcoding.plain.ui.components.TagNameDialog
import com.ismartcoding.plain.ui.models.TagsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchSelectTagsDialog(
    tagsVM: TagsViewModel,
    tagsState: List<DTag>,
    items: List<IData>,
    removeFromTags: Boolean = false,
    onDismiss: () -> Unit,
) {
    TagNameDialog(tagsVM)
    val tagIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            onDismiss()
        },
        title = {
            Text(text = stringResource(id = if (removeFromTags) R.string.remove_from_tags else R.string.add_to_tags),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            FlowRow(
                modifier =
                Modifier
                    .fillMaxWidth(),
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
                    tagsVM.showAddDialog()
                })
            }
        }, confirmButton = {
            Button(
                enabled = tagIds.isNotEmpty(),
                onClick = {
                    if (removeFromTags) {
                        tagsVM.removeFromTags(items.map { it.id }.toSet(), tagIds.toSet())
                    } else {
                        tagsVM.addToTags(items, tagIds.toSet())
                    }
                    onDismiss()
                },
            ) {
                Text(stringResource(id = R.string.save))
            }
        }, dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        })
}


