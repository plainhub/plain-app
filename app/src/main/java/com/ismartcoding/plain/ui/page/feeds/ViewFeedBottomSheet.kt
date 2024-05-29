package com.ismartcoding.plain.ui.page.feeds

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.GroupButton
import com.ismartcoding.plain.ui.base.GroupButtons
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewFeedBottomSheet(
    viewModel: FeedsViewModel,
) {
    val m = viewModel.selectedItem.value ?: return
    LaunchedEffect(Unit) {
        viewModel.editFetchContent.value = m.fetchContent
    }

    val context = LocalContext.current
    val onDismiss = {
        viewModel.selectedItem.value = null
    }
    val groupButtons = remember { mutableStateListOf<GroupButton>() }
    LaunchedEffect(Unit) {
        groupButtons.addAll(
            listOf(
                GroupButton(
                    icon = Icons.Outlined.Checklist,
                    text = LocaleHelper.getString(R.string.select),
                    onClick = {
                        viewModel.enterSelectMode()
                        viewModel.select(m.id)
                        onDismiss()
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.Edit,
                    text = LocaleHelper.getString(R.string.edit),
                    onClick = {
                        viewModel.showEditDialog(m)
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
                )
            ))
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        GroupButtons(
            buttons = groupButtons
        )
        VerticalSpace(dp = 24.dp)
        Subtitle(text = m.name)
        PCard {
            PListItem(modifier = Modifier.clickable {
                WebHelper.open(context, m.url)
            }, title = m.url, separatedActions = true, action = {
                PIconButton(icon = Icons.Outlined.ContentCopy, contentDescription = stringResource(id = R.string.copy_link), onClick = {
                    val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), m.url)
                    clipboardManager.setPrimaryClip(clip)
                    DialogHelper.showTextCopiedMessage(m.url)
                })
            })
        }
        VerticalSpace(dp = 16.dp)
        PCard {
            PListItem(modifier = Modifier.clickable {
                viewModel.editFetchContent.value = !viewModel.editFetchContent.value
                m.fetchContent = viewModel.editFetchContent.value
                viewModel.updateFetchContent(m.id, viewModel.editFetchContent.value)
            }, title = stringResource(id = R.string.auto_fetch_full_content), action = {
                PSwitch(
                    activated = viewModel.editFetchContent.value,
                ) {
                    viewModel.editFetchContent.value = it
                    m.fetchContent = it
                    viewModel.updateFetchContent(m.id, it)
                }
            })
        }
        Tips(text = stringResource(id = R.string.auto_fetch_full_content_tips))
        VerticalSpace(dp = 16.dp)
        PCard {
            PListItem(title = stringResource(id = R.string.created_at), value = m.createdAt.formatDateTime())
            PListItem(title = stringResource(id = R.string.updated_at), value = m.updatedAt.formatDateTime())
        }
        BottomSpace()
    }
}


