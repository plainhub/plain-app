package com.ismartcoding.plain.ui.feeds

import com.ismartcoding.plain.platform.files.getFileNameFromUri
import com.ismartcoding.plain.platform.files.readTextFile
import com.ismartcoding.plain.features.files.FilesClient
import com.ismartcoding.plain.ui.i18n.LocaleHelper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ismartcoding.plain.features.files.ExportFileType
import com.ismartcoding.plain.features.files.PickFileTag
import com.ismartcoding.plain.events.ExportFileResultEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.CancellationException

@Composable
fun FeedsPageEffects(feedsVM: FeedsViewModel) {
    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.FEED) return@collect
                    DialogHelper.showLoading()
                    try {
                        val content = readTextFile(event.uris.first())
                        feedsVM.source.feeds.importAsync(content)
                        feedsVM.loadAsync(withCount = true)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        DialogHelper.showMessage(error)
                    } finally {
                        DialogHelper.hideLoading()
                    }
                }

                is ExportFileResultEvent -> {
                    if (event.type != ExportFileType.OPML) return@collect
                    try {
                        val opml = feedsVM.source.feeds.exportAsync()
                        check(FilesClient.writeChunks(event.uri, listOf(opml.encodeToByteArray()).iterator())) {
                            "Unable to write selected OPML file"
                        }
                        val fileName = getFileNameFromUri(event.uri) ?: ""
                        DialogHelper.showConfirmDialog(
                            "",
                            LocaleHelper.getStringFAsync(Res.string.exported_to, fileName),
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        DialogHelper.showMessage(error)
                    }
                }
            }
        }
    }
}
