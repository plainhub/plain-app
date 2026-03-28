package com.ismartcoding.plain.ui

import android.view.WindowManager
import com.ismartcoding.plain.ui.models.acceptChannelInvite
import com.ismartcoding.plain.ui.models.declineChannelInvite
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.capitalize
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.events.PairingResponseEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.locale.LocaleHelper.getStringF
import com.ismartcoding.plain.web.HttpServerManager
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun MainActivity.handleConfirmToAcceptLogin(event: ConfirmToAcceptLoginEvent) {
    try {
        val clientIp = HttpServerManager.clientIpCache[event.clientId] ?: ""
        if (requestToConnectDialog?.isShowing == true) {
            requestToConnectDialog?.dismiss()
            requestToConnectDialog = null
        }
        val r = event.request
        requestToConnectDialog = AlertDialog.Builder(MainActivity.instance.get()!!)
            .setTitle(getStringF(R.string.request_to_connect, "ip", clientIp))
            .setMessage(getStringF(R.string.client_ua, "os_name", r.osName.capitalize(), "os_version", r.osVersion, "browser_name", r.browserName.capitalize(), "browser_version", r.browserVersion))
            .setPositiveButton(getString(R.string.accept)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) { HttpServerManager.respondTokenAsync(event, clientIp) }
            }
            .setNegativeButton(getString(R.string.reject)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) { event.session.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "rejected")) }
            }.create()
        if (Permission.SYSTEM_ALERT_WINDOW.can(this)) {
            requestToConnectDialog?.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }
        requestToConnectDialog?.window?.setDimAmount(0.8f)
        requestToConnectDialog?.show()
    } catch (e: Exception) {
        LogCat.e("Error showing connect dialog: ${e.message}")
        requestToConnectDialog = null
    }
}

internal fun MainActivity.handlePairingRequest(event: PairingRequestReceivedEvent) {
    try {
        if (pairingRequestDialog?.isShowing == true) {
            pairingRequestDialog?.dismiss()
            pairingRequestDialog = null
        }
        val request = event.request
        pairingRequestDialog = AlertDialog.Builder(MainActivity.instance.get()!!)
            .setTitle(getString(R.string.pairing_request))
            .setMessage(getString(R.string.pairing_request_message, request.fromName))
            .setPositiveButton(getString(R.string.allow)) { _, _ -> sendEvent(PairingResponseEvent(request, event.fromIp, true)) }
            .setNegativeButton(getString(R.string.deny)) { _, _ -> sendEvent(PairingResponseEvent(request, event.fromIp, false)) }
            .setCancelable(false).create()
        if (Permission.SYSTEM_ALERT_WINDOW.can(this)) {
            pairingRequestDialog?.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }
        pairingRequestDialog?.window?.setDimAmount(0.8f)
        pairingRequestDialog?.show()
    } catch (e: Exception) {
        LogCat.e("Error showing pairing dialog: ${e.message}")
        pairingRequestDialog = null
    }
}

internal fun MainActivity.handleChannelInvite(event: ChannelInviteReceivedEvent) {
    try {
        if (channelInviteDialog?.isShowing == true) {
            channelInviteDialog?.dismiss()
            channelInviteDialog = null
        }
        channelInviteDialog = AlertDialog.Builder(MainActivity.instance.get()!!)
            .setTitle(getString(R.string.channel_invite))
            .setMessage(getString(R.string.channel_invite_message, event.ownerPeerName, event.channelName))
            .setPositiveButton(getString(R.string.accept)) { _, _ -> channelVM.acceptChannelInvite(event.channelId) }
            .setNegativeButton(getString(R.string.decline)) { _, _ -> channelVM.declineChannelInvite(this, event.channelId) }
            .setCancelable(false).create()
        if (Permission.SYSTEM_ALERT_WINDOW.can(this)) {
            channelInviteDialog?.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }
        channelInviteDialog?.window?.setDimAmount(0.8f)
        channelInviteDialog?.show()
    } catch (e: Exception) {
        LogCat.e("Error showing channel invite dialog: ${e.message}")
        channelInviteDialog = null
    }
}
