package com.ismartcoding.plain.ui.page.feeds

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PIconTextActionButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewFeedEntryBottomSheet(
    viewModel: FeedEntriesViewModel,
    tagsViewModel: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
) {
    val m = viewModel.selectedItem.value ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onDismiss = {
        viewModel.selectedItem.value = null
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        LazyColumn {
            item {
                ActionButtons {
                    if (!viewModel.showSearchBar.value) {
                        PIconTextActionButton(
                            icon = Icons.Outlined.Checklist,
                            text = LocaleHelper.getString(R.string.select),
                            click = {
                                viewModel.enterSelectMode()
                                viewModel.select(m.id)
                                onDismiss()
                            }
                        )
                    }
                    PIconTextActionButton(
                        icon = Icons.Outlined.DeleteForever,
                        text = LocaleHelper.getString(R.string.delete),
                        click = {
                            viewModel.delete(tagsViewModel, setOf(m.id))
                            onDismiss()
                        }
                    )
                }
                VerticalSpace(dp = 16.dp)
                Subtitle(text = stringResource(id = R.string.tags))
                TagSelector(
                    data = m,
                    tagsViewModel = tagsViewModel,
                    tagsMap = tagsMap,
                    tagsState = tagsState,
                    onChanged = {
                        scope.launch(Dispatchers.IO) {
                            viewModel.refreshTabsAsync(tagsViewModel)
                        }
                    }
                )
                VerticalSpace(dp = 24.dp)
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
                    PListItem(title = stringResource(id = R.string.published_at), value = m.publishedAt.formatDateTime())
                    PListItem(title = stringResource(id = R.string.created_at), value = m.createdAt.formatDateTime())
                    PListItem(title = stringResource(id = R.string.updated_at), value = m.updatedAt.formatDateTime())
                }
                BottomSpace()
            }
        }
    }
}


