package com.ismartcoding.plain.platform

import com.ismartcoding.plain.i18n.*

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.lib.extensions.queryOpenableFileName
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.contentResolver
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.events.ExportFileResultEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.feed.exportAsync
import com.ismartcoding.plain.features.feed.importAsync
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedsViewModel
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Composable
actual fun FeedsPageEffects(feedsVM: FeedsViewModel) {
    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.FEED) return@collect
                    val uri = Uri.parse(event.uris.first())
                    InputStreamReader(contentResolver.openInputStream(uri)!!).use { reader ->
                        DialogHelper.showLoading()
                        withIO {
                            try {
                                FeedHelper.importAsync(reader)
                                feedsVM.loadAsync(withCount = true)
                                DialogHelper.hideLoading()
                            } catch (ex: Exception) {
                                DialogHelper.hideLoading()
                                DialogHelper.showMessage(ex.toString())
                            }
                        }
                    }
                }
                is ExportFileResultEvent -> {
                    if (event.type == ExportFileType.OPML) {
                        OutputStreamWriter(contentResolver.openOutputStream(event.uri)!!, Charsets.UTF_8).use { writer ->
                            withIO { FeedHelper.exportAsync(writer) }
                        }
                        val fileName = contentResolver.queryOpenableFileName(event.uri)
                        DialogHelper.showConfirmDialog("", LocaleHelper.getStringFAsync(Res.string.exported_to, "name", fileName))
                    }
                }
            }
        }
    }
}
