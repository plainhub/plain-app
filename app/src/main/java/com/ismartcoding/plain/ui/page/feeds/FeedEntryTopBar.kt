package com.ismartcoding.plain.ui.page.feeds

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.cut
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.FeedEntryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun FeedEntryTopBar(
    navController: NavHostController, feedEntryVM: FeedEntryViewModel,
    scrollBehavior: TopAppBarScrollBehavior, scope: CoroutineScope, context: Context,
    onScrollToTop: () -> Unit,
) {
    PTopAppBar(
        modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { onScrollToTop() }),
        navController = navController, title = "", scrollBehavior = scrollBehavior,
        actions = {
            PIconButton(icon = R.drawable.label, contentDescription = stringResource(R.string.select_tags), tint = MaterialTheme.colorScheme.onSurface) {
                feedEntryVM.showSelectTagsDialog.value = true
            }
            PIconButton(icon = R.drawable.chrome, contentDescription = stringResource(R.string.open_in_web), tint = MaterialTheme.colorScheme.onSurface) {
                val m = feedEntryVM.item.value ?: return@PIconButton; WebHelper.open(context, m.url)
            }
            PIconButton(icon = R.drawable.share_2, contentDescription = stringResource(R.string.share), tint = MaterialTheme.colorScheme.onSurface) {
                val m = feedEntryVM.item.value ?: return@PIconButton; ShareHelper.shareText(context, m.title.let { it + "\n" } + m.url)
            }
            ActionButtonMoreWithMenu { dismiss ->
                PDropdownMenuItem(text = { Text(stringResource(R.string.save_to_notes)) },
                    leadingIcon = { Icon(painter = painterResource(R.drawable.save), contentDescription = stringResource(id = R.string.save_to_notes)) },
                    onClick = {
                        dismiss(); val m = feedEntryVM.item.value ?: return@PDropdownMenuItem
                        scope.launch(Dispatchers.IO) {
                            val c = "# ${m.title}\n\n" + m.content.ifEmpty { m.description }
                            NoteHelper.saveToNotesAsync(m.id) { title = c.cut(250).replace("\n", ""); content = c }
                            DialogHelper.showMessage(R.string.saved)
                        }
                    })
                PDropdownMenuItem(text = { Text(stringResource(R.string.copy_link)) },
                    leadingIcon = { Icon(painter = painterResource(R.drawable.link), contentDescription = stringResource(id = R.string.copy_link)) },
                    onClick = {
                        dismiss(); val m = feedEntryVM.item.value ?: return@PDropdownMenuItem
                        val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), m.url)
                        clipboardManager.setPrimaryClip(clip); DialogHelper.showTextCopiedMessage(m.url)
                    })
            }
        },
    )
}
