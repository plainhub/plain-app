package com.ismartcoding.plain.ui

import android.content.Intent
import android.net.Uri
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.extensions.parcelableArrayList
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import com.ismartcoding.plain.web.models.toModel
import kotlinx.coroutines.delay

internal fun MainActivity.handleIntent(intent: Intent) {
    if (intent.getBooleanExtra("start_web_service", false)) {
        coIO {
            WebPreference.putAsync(this@handleIntent, true)
            sendEvent(StartHttpServerEvent())
        }
    }

    if (intent.action == Intent.ACTION_VIEW) {
        val uri = intent.data ?: return
        val mimeType = contentResolver.getType(uri)
        if (mimeType != null) {
            if (mimeType.startsWith("text/")) navControllerState.value?.navigateTextFile(uri.toString())
            else if (mimeType == "application/pdf") navControllerState.value?.navigatePdf(uri)
            else DialogHelper.showErrorMessage(LocaleHelper.getString(R.string.not_supported_error))
        } else {
            DialogHelper.showErrorMessage(LocaleHelper.getString(R.string.not_supported_error))
        }
    } else if (intent.action == Intent.ACTION_SEND) {
        if (intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            coMain {
                val item = withIO { ChatDbHelper.sendAsync(DMessageContent(DMessageType.TEXT.value, DMessageText(sharedText))) }
                val m = item.toModel()
                m.data = m.getContentData()
                sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(arrayListOf(m))))
                navControllerState.value?.navigate(Routing.Chat("local"))
            }
            return
        }
        val uri = intent.parcelable(Intent.EXTRA_STREAM) as? Uri ?: return
        coMain {
            DialogHelper.showLoading()
            withIO { peerVM.loadPeers() }
            DialogHelper.hideLoading()
            pendingFileUris = setOf(uri)
            showForwardTargetDialog = true
        }
    } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
        val uris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
        if (uris != null) {
            coMain {
                DialogHelper.showLoading()
                withIO { peerVM.loadPeers() }
                DialogHelper.hideLoading()
                pendingFileUris = uris.toSet()
                showForwardTargetDialog = true
            }
        }
    } else if (intent.action == Constants.ACTION_PLAY_MEDIA) {
        val path = intent.getStringExtra(Constants.EXTRA_MEDIA_PATH) ?: return
        navControllerState.value?.navigate(Routing.PlayMedia(path))
    }
}
