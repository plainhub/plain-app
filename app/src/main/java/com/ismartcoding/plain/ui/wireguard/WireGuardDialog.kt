package com.ismartcoding.plain.ui.wireguard

import android.os.Bundle
import android.view.View
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogWireguardBinding
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.box.ApplyWireGuardResultEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.wireguard.WireGuard
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.views.ListItemView

class WireGuardDialog(var wireGuard: WireGuard) : BaseBottomSheetDialog<DialogWireguardBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.run {
            title = wireGuard.interfaze.name
            initMenu(R.menu.more)
            onMenuItemClick {
                when (itemId) {
                    R.id.more -> {
                        WireGuardConfigDialog(wireGuard).show()
                    }
                }
            }
        }

        receiveEvent<ApplyWireGuardResultEvent> {
            UIDataCache.current().wireGuards?.let { wgs ->
                val wg = wgs.find { it.id == wireGuard.id }
                if (wg != null) {
                    wireGuard = wg
                }
            }
            updateUI()
        }

        updateUI()
    }

    private fun updateUI() {
        binding.iface.run {
            clearTextRows()
            setKeyText("${getString(R.string._interface)}: " + wireGuard.id)
            addTextRow("${getString(R.string.ip_address)}: " + wireGuard.interfaze.addresses.joinToString(", "))
            addTextRow("${getString(R.string.public_key)}: " + wireGuard.interfaze.keyPair.publicKey.toBase64())
            wireGuard.listeningPort?.let {
                addTextRow("${getString(R.string.listening_port)}: $it")
            }
        }

        binding.peers.removeAllViews()
        wireGuard.peers.forEach { peer ->
            val view = ListItemView(requireContext(), null)
            view.setKeyText("${getString(R.string.peer)}: " + peer.getDisplayName())
            view.addTextRow("${getString(R.string.allowed_ips)}: " + peer.allowedIps.joinToString(", "))
            if (peer.latestHandshake != null) {
                view.addTextRow("${getString(R.string.endpoint)}: " + peer.endpointing)
                view.addTextRow("${getString(R.string.latest_handshake)}: " + peer.latestHandshake?.formatDateTime())
                view.addTextRow(
                    LocaleHelper.getStringF(
                        R.string.transfer_text,
                        "rx_bytes",
                        FormatHelper.formatBytes(peer.rxBytes),
                        "tx_bytes",
                        FormatHelper.formatBytes(peer.txBytes),
                    ),
                )
            }
            binding.peers.addView(view)
        }
    }
}
