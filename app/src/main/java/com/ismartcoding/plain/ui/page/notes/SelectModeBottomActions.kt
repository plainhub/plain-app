package com.ismartcoding.plain.ui.page.notes

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.LabelOff
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.PIconTextActionButton
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.getSelectedItems
import com.ismartcoding.plain.ui.page.tags.BatchSelectTagsDialog
import com.ismartcoding.plain.ui.theme.bottomAppBarContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectModeBottomActions(
    viewModel: NotesViewModel,
    tagsViewModel: TagsViewModel,
    tagsState: List<DTag>,
) {
    var showSelectTagsDialog by remember {
        mutableStateOf(false)
    }
    var removeFromTags by remember {
        mutableStateOf(false)
    }

    if (showSelectTagsDialog) {
        BatchSelectTagsDialog(tagsViewModel, tagsState, viewModel.getSelectedItems(), removeFromTags) {
            showSelectTagsDialog = false
            viewModel.exitSelectMode()
        }
    }

    BottomAppBar(
        modifier = Modifier.height(120.dp),
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.bottomAppBarContainer(),
    ) {
        ActionButtons {
            if (viewModel.trash.value) {
                PIconTextActionButton(
                    icon = Icons.Outlined.RestoreFromTrash,
                    text = LocaleHelper.getString(R.string.restore),
                    click = {
                        viewModel.restore(tagsViewModel, viewModel.selectedIds.toSet())
                        viewModel.exitSelectMode()
                    }
                )
                PIconTextActionButton(
                    icon = Icons.Outlined.DeleteForever,
                    text = LocaleHelper.getString(R.string.delete),
                    click = {
                        viewModel.delete(tagsViewModel, viewModel.selectedIds.toSet())
                        viewModel.exitSelectMode()
                    }
                )
            } else {
                PIconTextActionButton(
                    icon = Icons.AutoMirrored.Outlined.Label,
                    text = LocaleHelper.getString(R.string.add_to_tags),
                    click = {
                        showSelectTagsDialog = true
                        removeFromTags = false
                    }
                )
                PIconTextActionButton(
                    icon = Icons.AutoMirrored.Outlined.LabelOff,
                    text = LocaleHelper.getString(R.string.remove_from_tags),
                    click = {
                        showSelectTagsDialog = true
                        removeFromTags = true
                    }
                )
                PIconTextActionButton(
                    icon = Icons.Outlined.DeleteOutline,
                    text = LocaleHelper.getString(R.string.move_to_trash),
                    click = {
                        viewModel.trash(tagsViewModel, viewModel.selectedIds.toSet())
                        viewModel.exitSelectMode()
                    }
                )
            }
        }
    }
}