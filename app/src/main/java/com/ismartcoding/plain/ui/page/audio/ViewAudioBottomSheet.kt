package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.GroupButton
import com.ismartcoding.plain.ui.base.GroupButtons
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSelectionChip
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.NewTagButton
import com.ismartcoding.plain.ui.components.TagNameDialog
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewAudioBottomSheet(
    viewModel: AudioViewModel,
    tagsViewModel: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
) {
    val m = viewModel.selectedItem.value ?: return
    val groupButtons = remember { mutableStateListOf<GroupButton>() }
    val tagIds = remember {
        mutableStateListOf<String>()
    }
    val onDismiss = {
        viewModel.selectedItem.value = null
    }
    LaunchedEffect(Unit) {
        tagIds.addAll(tagsMap[m.id]?.map { it.tagId } ?: emptyList())
        if (!viewModel.search.value) {
            groupButtons.add(
                GroupButton(
                    icon = Icons.Outlined.Checklist,
                    text = LocaleHelper.getString(R.string.select),
                    onClick = {
                        viewModel.enterSelectMode()
                        viewModel.select(m.id)
                        onDismiss()
                    }
                )
            )
        }
        if (viewModel.trash.value) {
            groupButtons.addAll(listOf(
                GroupButton(
                    icon = Icons.Outlined.RestoreFromTrash,
                    text = LocaleHelper.getString(R.string.restore),
                    onClick = {
                        viewModel.untrash(setOf(m.id))
                        onDismiss()
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.DeleteForever,
                    text = LocaleHelper.getString(R.string.delete),
                    onClick = {
                        viewModel.delete(setOf(m.id))
                        onDismiss()
                    }
                ),
            ))
        } else {
            groupButtons.addAll(listOf(
//            GroupButton(
//                icon = painterResource(R.drawable.ic_keep),
//                text = stringResource(id = R.string.pin),
//                onClick = {
//                }
//            ),
                GroupButton(
                    icon = Icons.Outlined.DeleteOutline,
                    text = LocaleHelper.getString(R.string.move_to_trash),
                    onClick = {
                        viewModel.trash(setOf(m.id))
                        onDismiss()
                    }
                ),
            ))
        }
    }


    TagNameDialog(tagsViewModel)

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        GroupButtons(
            buttons = groupButtons
        )
        if (!viewModel.trash.value) {
            VerticalSpace(dp = 16.dp)
            Subtitle(text = stringResource(id = R.string.tags))
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
                            tagsViewModel.toggleTag(m.id, tag.id)
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
                    tagsViewModel.showAddDialog()
                })
            }
            VerticalSpace(dp = 24.dp)
        }
        BottomSpace()
    }
}


