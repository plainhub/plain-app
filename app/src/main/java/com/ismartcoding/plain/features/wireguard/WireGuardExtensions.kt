package com.ismartcoding.plain.features.wireguard

import androidx.lifecycle.LifecycleCoroutineScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.ApplyWireGuardMutation
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.box.ApplyWireGuardResultEvent
import com.ismartcoding.plain.fragment.WireGuardFragment
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.wireguard.WireGuardDialog
import kotlinx.coroutines.launch

fun WireGuardFragment.toWireGuard(): WireGuard {
    val wg = WireGuard()
    wg.parse(this.config)
    wg.id = this.id
    wg.isActive = this.isActive
    wg.isEnabled = this.isEnabled
    wg.listeningPort = this.listeningPort
    wg.peers.forEach { peer ->
        val peerStats = peers.find { it.publicKey == peer.publicKey?.toBase64() }
        if (peerStats != null) {
            peer.rxBytes = peerStats.rxBytes.toLong()
            peer.txBytes = peerStats.txBytes.toLong()
            peer.latestHandshake = peerStats.latestHandshake
            peer.endpointing = peerStats.endpoint
        }
    }
    return wg
}

fun ViewListItemBinding.bindWireGuard(
    lifecycleScope: LifecycleCoroutineScope,
    item: WireGuard,
) {
    clearTextRows()
    setKeyText(if (item.interfaze.name.isEmpty()) item.id else item.interfaze.name)
    addTextRow(item.interfaze.addresses.joinToString(", "))
    setClick {
        WireGuardDialog(item).show()
    }
    setSwitch(item.isEnabled, onChanged = { _, isEnabled ->
        lifecycleScope.launch {
            DialogHelper.showLoading()
            val r =
                withIO {
                    BoxApi.mixMutateAsync(ApplyWireGuardMutation(item.id, item.raw, isEnabled))
                }
            DialogHelper.hideLoading()
            if (!r.isSuccess()) {
                DialogHelper.showErrorDialog(r.getErrorMessage())
                setSwitchEnable(!isEnabled)
                return@launch
            }

            r.response?.data?.applyWireGuard?.wireGuardFragment?.let { wg ->
                UIDataCache.current().wireGuards?.run {
                    val index = indexOfFirst { it.id == item.id }
                    if (index != -1) {
                        removeAt(index)
                        add(index, wg.toWireGuard())
                    }
                }
            }
            sendEvent(ApplyWireGuardResultEvent(TempData.selectedBoxId, r))
        }
    })
}
