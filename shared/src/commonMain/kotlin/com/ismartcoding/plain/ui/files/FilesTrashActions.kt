package com.ismartcoding.plain.ui.files

import androidx.compose.runtime.*
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.PIconTextSmallButton
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore
import com.ismartcoding.plain.ui.resources.delete_forever
import com.ismartcoding.plain.ui.state.launchSafe
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FilesTrashActions(source: ContentSource, selected: Set<String>) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    fun apply(paths: Set<String>, purge: Boolean) {
        if (busy) return
        busy = true
        scope.launchSafe(onDone = { busy = false }) {
            if (purge) source.files.deleteTrashed(paths)
            else source.files.restore(paths)
        }
    }
    PBottomAppBar {
        BottomActionButtons {
            PIconTextSmallButton(UiRes.drawable.archive_restore, stringResource(Res.string.restore), enabled = !busy) {
                apply(selected, false)
            }
            PIconTextSmallButton(UiRes.drawable.delete_forever, stringResource(Res.string.delete), enabled = !busy) {
                scope.launchSafe {
                    confirmActionAsync(Res.string.delete, Res.string.confirm_to_delete,
                        callback = { apply(selected, true) }, danger = true)
                }
            }
        }
    }
}
