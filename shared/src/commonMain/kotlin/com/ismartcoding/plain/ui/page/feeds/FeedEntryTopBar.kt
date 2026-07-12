package com.ismartcoding.plain.ui.page.feeds

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.cut
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.platform.shareText
import com.ismartcoding.plain.platform.launchUrl
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedEntryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun FeedEntryTopBar(
    navController: NavHostController, feedEntryVM: FeedEntryViewModel,
    scrollBehavior: TopAppBarScrollBehavior, scope: CoroutineScope,
    onScrollToTop: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    PTopAppBar(
        modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { onScrollToTop() }),
        navController = navController, title = "", scrollBehavior = scrollBehavior,
        actions = {
            PIconButton(icon = Res.drawable.label, contentDescription = stringResource(Res.string.select_tags), tint = MaterialTheme.colorScheme.onSurface) {
                feedEntryVM.showSelectTagsDialog.value = true
            }
            PIconButton(icon = Res.drawable.chrome, contentDescription = stringResource(Res.string.open_in_web), tint = MaterialTheme.colorScheme.onSurface) {
                val m = feedEntryVM.item.value ?: return@PIconButton
                try { launchUrl(m.url) } catch (_: Exception) { DialogHelper.showMessage(Res.string.no_browser_error) }
            }
            PIconButton(icon = Res.drawable.share_2, contentDescription = stringResource(Res.string.share), tint = MaterialTheme.colorScheme.onSurface) {
                val m = feedEntryVM.item.value ?: return@PIconButton
                shareText(m.title.let { it + "\n" } + m.url)
            }
            ActionButtonMoreWithMenu { dismiss ->
                PDropdownMenuItem(text = { Text(stringResource(Res.string.save_to_notes)) },
                    leadingIcon = { Icon(painter = painterResource(Res.drawable.save), contentDescription = stringResource(Res.string.save_to_notes)) },
                    onClick = {
                        dismiss(); val m = feedEntryVM.item.value ?: return@PDropdownMenuItem
                        scope.launch(IODispatcher) {
                            val c = "# ${m.title}\n\n" + m.content.ifEmpty { m.description }
                            NoteHelper.saveToNotesAsync(m.id) { title = c.cut(250).replace("\n", ""); content = c }
                            DialogHelper.showMessage(Res.string.saved)
                        }
                    })
                PDropdownMenuItem(text = { Text(stringResource(Res.string.copy_link)) },
                    leadingIcon = { Icon(painter = painterResource(Res.drawable.link), contentDescription = stringResource(Res.string.copy_link)) },
                    onClick = {
                        dismiss(); val m = feedEntryVM.item.value ?: return@PDropdownMenuItem
                        clipboard.setText(AnnotatedString(m.url))
                        DialogHelper.showTextCopiedMessage(m.url)
                    })
            }
        },
    )
}
