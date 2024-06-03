package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.GroupButton
import com.ismartcoding.plain.ui.base.GroupButtons
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.models.AudioViewModel
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
    val onDismiss = {
        viewModel.selectedItem.value = null
    }
    LaunchedEffect(Unit) {
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
            TagSelector(
                data = m,
                tagsViewModel = tagsViewModel,
                tagsMap = tagsMap,
                tagsState = tagsState,
                onChanged = {}
            )
            VerticalSpace(dp = 24.dp)
        }
        BottomSpace()
    }
}


