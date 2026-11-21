package com.ismartcoding.plain.ui.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.features.files.TrashedFile
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.rust.graphql.*
import com.ismartcoding.plain.ui.base.PListItem
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
    var entries by remember(source) { mutableStateOf<List<TrashedFile>>(emptyList()) }
    var selected by remember(source) { mutableStateOf(emptySet<String>()) }
    var busy by remember(source) { mutableStateOf(false) }
    var loaded by remember(source) { mutableStateOf(false) }

    PBackHandler(enabled = true, onBack = onBack)
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
        PTopAppBar(onNavigateBack = onBack, title = stringResource(Res.string.trash), actions = {
            if (selected.isNotEmpty()) {
                PTopRightButton(stringResource(Res.string.restore), { apply(selected, false) }, enabled = !busy)
                PTopRightButton(stringResource(Res.string.permanently_delete_file), {
                    val ids = selected
                    scope.launchSafe {
                        confirmActionAsync(Res.string.permanently_delete_file, Res.string.confirm_to_delete,
                            callback = { apply(ids, true) }, danger = true)
                    }
                }, enabled = !busy)
            }
        })
    }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            if (loaded && entries.isEmpty()) item { PListItem(title = stringResource(Res.string.no_data)) }
            items(entries, key = { it.id }) { item ->
                PListItem(
                    modifier = Modifier.clickable {
                        selected = if (item.id in selected) selected - item.id else selected + item.id
                    },
                    title = item.displayName, subtitle = item.originalPath,
                    start = { Checkbox(checked = item.id in selected, onCheckedChange = null) },
                )
            }
        }
    }
}
