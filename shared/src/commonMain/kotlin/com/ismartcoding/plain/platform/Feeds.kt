package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.events.ExportFileResultEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.feed.exportAsync
import com.ismartcoding.plain.features.feed.importAsync
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedsViewModel

/**
 * Side-effect handler for the Feeds page: listens for OPML pick/export events
 * on the global [Channel.sharedFlow] and dispatches them through [FeedHelper].
 *
 * File I/O goes through the low-level URI expects ([readTextFile],
 * [writeBytesToUri], [getFileNameFromUri]); the event-handling structure is
 * identical across platforms. iOS does not emit pick/export events today, so
 * the collectors remain dormant but structurally unified.
 */
@Composable
fun FeedsPageEffects(feedsVM: FeedsViewModel) {
    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.FEED) return@collect
                    val content = readTextFile(event.uris.first())
                    DialogHelper.showLoading()
                    withIO {
                        try {
                            FeedHelper.importAsync(content)
                            feedsVM.loadAsync(withCount = true)
                            DialogHelper.hideLoading()
                        } catch (ex: Exception) {
                            DialogHelper.hideLoading()
                            DialogHelper.showMessage(ex)
                        }
                    }
                }

                is ExportFileResultEvent -> {
                    if (event.type != ExportFileType.OPML) return@collect
                    val opml = withIO { FeedHelper.exportAsync() }
                    writeBytesToUri(event.uri, opml.encodeToByteArray())
                    val fileName = getFileNameFromUri(event.uri) ?: ""
                    DialogHelper.showConfirmDialog(
                        "",
                        LocaleHelper.getStringFAsync(Res.string.exported_to, "name", fileName),
                    )
                }
            }
        }
    }
}
