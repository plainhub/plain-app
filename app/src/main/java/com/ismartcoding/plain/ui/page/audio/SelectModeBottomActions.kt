package com.ismartcoding.plain.ui.page.audio

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.GroupButton
import com.ismartcoding.plain.ui.base.GroupButtons
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.getSelectedItems
import com.ismartcoding.plain.ui.page.tags.BatchSelectTagsDialog
import com.ismartcoding.plain.ui.theme.bottomAppBarContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectModeBottomActions(
    viewModel: AudioViewModel,
    tagsViewModel: TagsViewModel,
    tagsState: List<DTag>,
) {
    val groupButtons = remember { mutableStateListOf<GroupButton>() }
    var showSelectTagsDialog by remember {
        mutableStateOf(false)
    }
    var removeFromTags by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        if (viewModel.trash.value) {
            groupButtons.addAll(listOf(
                GroupButton(
                    icon = Icons.Outlined.RestoreFromTrash,
                    text = LocaleHelper.getString(R.string.restore),
                    onClick = {
                        viewModel.restore(viewModel.selectedIds.toSet())
                        viewModel.exitSelectMode()
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.DeleteForever,
                    text = LocaleHelper.getString(R.string.delete),
                    onClick = {
                        viewModel.delete(viewModel.selectedIds.toSet())
                        viewModel.exitSelectMode()
                    }
                ),
            ))
        } else {
            groupButtons.addAll(listOf(
                GroupButton(
                    icon = Icons.AutoMirrored.Outlined.Label,
                    text = LocaleHelper.getString(R.string.add_to_tags),
                    onClick = {
                        showSelectTagsDialog = true
                        removeFromTags = false
                    }
                ),
                GroupButton(
                    icon = Icons.AutoMirrored.Outlined.LabelOff,
                    text = LocaleHelper.getString(R.string.remove_from_tags),
                    onClick = {
                        showSelectTagsDialog = true
                        removeFromTags = true
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.DeleteOutline,
                    text = LocaleHelper.getString(R.string.move_to_trash),
                    onClick = {
                        viewModel.trash(viewModel.selectedIds.toSet())
                        viewModel.exitSelectMode()
                    }
                ),
            ))
        }
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
        GroupButtons(
            buttons = groupButtons
        )
    }
}