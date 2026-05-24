package com.ismartcoding.plain.ui

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavDestination.Companion.hasRoute
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.extensions.parcelableArrayList
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.sendTextMessage
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import com.ismartcoding.plain.ui.page.chat.components.ForwardTarget
import kotlinx.coroutines.delay

internal fun MainActivity.handleIntent(intent: Intent) {
    if (intent.getBooleanExtra("start_web_service", false)) {
        coIO {
            WebPreference.putAsync(true)
            sendEvent(StartHttpServerEvent())
        }
    }

    if (intent.getBooleanExtra("navigate_to_web_settings", false)) {
        val nav = navControllerState.value
        val alreadyThere = nav?.currentBackStackEntry?.destination?.hasRoute(Routing.WebSettings::class) == true
        if (!alreadyThere) nav?.navigate(Routing.WebSettings)
    }

    if (intent.action == Intent.ACTION_VIEW) {
        val uri = intent.data ?: return
        val mimeType = contentResolver.getType(uri)
        if (mimeType != null) {
            if (mimeType.startsWith("text/")) navControllerState.value?.navigateTextFile(uri.toString())
            else if (mimeType == "application/pdf") navControllerState.value?.navigatePdf(uri)
            else DialogHelper.showErrorMessage(LocaleHelper.getStringSync(Res.string.not_supported_error))
        } else {
            DialogHelper.showErrorMessage(LocaleHelper.getStringSync(Res.string.not_supported_error))
        }
    } else if (intent.action == Intent.ACTION_SEND) {
        if (intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            pendingFileUris = null
            pendingForwardText = sharedText
            showForwardTargetOptions(peerVM)
            return
        }
        val uri = intent.parcelable(Intent.EXTRA_STREAM) as? Uri ?: return
        pendingFileUris = setOf(uri)
        pendingForwardText = null
        showForwardTargetOptions(peerVM)
    } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
        val uris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
        if (uris != null) {
            pendingFileUris = uris.toSet()
            pendingForwardText = null
            showForwardTargetOptions(peerVM)
        }
    } else if (intent.action == Constants.ACTION_PLAY_MEDIA) {
        val path = intent.getStringExtra(Constants.EXTRA_MEDIA_PATH) ?: return
        navControllerState.value?.navigate(Routing.PlayMedia(path))
    }
}

private fun MainActivity.showForwardTargetOptions(peerVM: PeerViewModel) {
    coMain {
        DialogHelper.showLoading()
        withIO { peerVM.loadPeers() }
        DialogHelper.hideLoading()
        showForwardTargetDialog = true
    }
}
