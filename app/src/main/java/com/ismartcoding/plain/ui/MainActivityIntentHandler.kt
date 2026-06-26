package com.ismartcoding.plain.ui

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavDestination.Companion.hasRoute
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.extensions.parcelable
import com.ismartcoding.plain.lib.extensions.parcelableArrayList
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.not_supported_error
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile

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
            else if (mimeType == "application/pdf") navControllerState.value?.navigatePdf(uri.toString())
            else DialogHelper.showErrorMessage(LocaleHelper.getString(Res.string.not_supported_error))
        } else {
            DialogHelper.showErrorMessage(LocaleHelper.getString(Res.string.not_supported_error))
        }
    } else if (intent.action == Intent.ACTION_SEND) {
        if (intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            pendingFileUris = null
            pendingForwardText = sharedText
            showForwardTargetDialog = true
            return
        }
        val uri = intent.parcelable(Intent.EXTRA_STREAM) as? Uri ?: return
        pendingFileUris = setOf(uri)
        pendingForwardText = null
        showForwardTargetDialog = true
    } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
        val uris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
        if (uris != null) {
            pendingFileUris = uris.toSet()
            pendingForwardText = null
            showForwardTargetDialog = true
        }
    } else if (intent.action == Constants.ACTION_PLAY_MEDIA) {
        val path = intent.getStringExtra(Constants.EXTRA_MEDIA_PATH) ?: return
        navControllerState.value?.navigate(Routing.PlayMedia(path))
    }
}
