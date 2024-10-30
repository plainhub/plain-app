package com.ismartcoding.plain.ui.page.feeds

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextEditButton
import com.ismartcoding.plain.ui.base.IconTextSelectButton
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
    feedsVM: FeedsViewModel,
) {
    val m = feedsVM.selectedItem.value ?: return
    LaunchedEffect(Unit) {
        feedsVM.editFetchContent.value = m.fetchContent
    }

    val context = LocalContext.current
    val onDismiss = {
        feedsVM.selectedItem.value = null
    }


    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        VerticalSpace(32.dp)
        ActionButtons {
            IconTextSelectButton {
                feedsVM.enterSelectMode()
                feedsVM.select(m.id)
                onDismiss()
            }
            IconTextEditButton {
                feedsVM.showEditDialog(m)
                onDismiss()
            }
            IconTextDeleteButton {
                feedsVM.delete(setOf(m.id))
                onDismiss()
            }
        }
        VerticalSpace(dp = 24.dp)
        Subtitle(text = m.name)
        PCard {
            PListItem(modifier = Modifier.clickable {
                WebHelper.open(context, m.url)
            }, title = m.url, separatedActions = true, action = {
                PIconButton(icon = R.drawable.copy, contentDescription = stringResource(id = R.string.copy_link), click = {
                    val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), m.url)
                    clipboardManager.setPrimaryClip(clip)
                    DialogHelper.showTextCopiedMessage(m.url)
                })
            })
        }
        VerticalSpace(dp = 16.dp)
        PCard {
            PListItem(modifier = Modifier.clickable {
                feedsVM.editFetchContent.value = !feedsVM.editFetchContent.value
                m.fetchContent = feedsVM.editFetchContent.value
                feedsVM.updateFetchContent(m.id, feedsVM.editFetchContent.value)
            }, title = stringResource(id = R.string.auto_fetch_full_content), action = {
                PSwitch(
                    activated = feedsVM.editFetchContent.value,
                ) {
                    feedsVM.editFetchContent.value = it
                    m.fetchContent = it
                    feedsVM.updateFetchContent(m.id, it)
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


