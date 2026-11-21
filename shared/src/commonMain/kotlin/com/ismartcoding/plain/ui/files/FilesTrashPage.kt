package com.ismartcoding.plain.ui.files

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.features.files.TrashedFile
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.rust.graphql.*
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.PIconTextSmallButton
import com.ismartcoding.plain.ui.components.NavigationCloseIcon
import com.ismartcoding.plain.ui.components.NoDataView
import com.ismartcoding.plain.ui.files.components.FileTrashItem
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore
import com.ismartcoding.plain.ui.resources.delete_forever
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilesTrashPage(source: ContentSource, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val previewerState = rememberPreviewerState()
    var entries by remember(source) { mutableStateOf<List<TrashedFile>>(emptyList()) }
    var selected by remember(source) { mutableStateOf(emptySet<String>()) }
    var busy by remember(source) { mutableStateOf(false) }
    var loaded by remember(source) { mutableStateOf(false) }

    val selecting = selected.isNotEmpty()
    val back: () -> Unit = { if (selecting) selected = emptySet() else onBack() }
    PBackHandler(onBack = back)
    LaunchedEffect(source) {
        val updates = when (source) {
            ContentSource.Local -> merge(AppSubscriptions.contentChanged, AppSubscriptions.mediaUpdated)
            is ContentSource.Remote -> source.client.subscriptionUpdates(listOf(GraphqlOperation("subscription { contentChanged }"), GraphqlOperation("subscription { mediaUpdated { types } }")))
        }
        updates.collect { update ->
            if (update is SubscriptionUpdate.Failed) {
                DialogHelper.showMessage(update.error)
            } else {
                try {
                    entries = source.files.trashed()
                    selected = selected.intersect(entries.map { it.id }.toSet())
                    loaded = true
                } catch (canceled: CancellationException) { throw canceled }
                catch (error: Exception) { DialogHelper.showMessage(error) }
            }
        }
    }
    fun apply(ids: Set<String>, purge: Boolean) {
        if (busy) return
        busy = true
        scope.launchSafe(onDone = { busy = false }) {
            if (purge) source.files.deleteTrashed(ids)
            else source.files.restore(ids)
        }
    }
    PScaffold(topBar = {
        PTopAppBar(
            onNavigateBack = back,
            navigationIcon = if (selecting) ({ NavigationCloseIcon(back) }) else null,
            title = if (selecting) stringResource(Res.string.x_selected, selected.size) else stringResource(Res.string.trash),
            actions = {
                if (selecting) {
                    val allSelected = selected.size == entries.size
                    PTopRightButton(
                        label = stringResource(if (allSelected) Res.string.unselect_all else Res.string.select_all),
                        click = { selected = if (allSelected) emptySet() else entries.map { it.id }.toSet() },
                    )
                }
            },
        )
    }, bottomBar = {
        if (selecting) PBottomAppBar {
            BottomActionButtons {
                PIconTextSmallButton(UiRes.drawable.archive_restore, stringResource(Res.string.restore), enabled = !busy) {
                    apply(selected, false)
                }
                PIconTextSmallButton(UiRes.drawable.delete_forever, stringResource(Res.string.delete), enabled = !busy) {
                    val ids = selected
                    scope.launchSafe {
                        confirmActionAsync(Res.string.delete, Res.string.confirm_to_delete,
                            callback = { apply(ids, true) }, danger = true)
                    }
                }
            }
        }
    }) { padding ->
        if (loaded && entries.isEmpty()) {
            NoDataView(modifier = Modifier.padding(padding), icon = null)
        } else {
            LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(entries, key = { it.id }) { item ->
                    FileTrashItem(item, item.id in selected, selecting, previewerState) {
                        selected = if (item.id in selected) selected - item.id else selected + item.id
                    }
                }
            }
        }
    }
}
