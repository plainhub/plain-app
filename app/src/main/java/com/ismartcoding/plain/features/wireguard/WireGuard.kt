package com.ismartcoding.plain.features.wireguard

import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.features.wireguard.config.Interface
import com.ismartcoding.plain.features.wireguard.config.Peer
import com.ismartcoding.plain.features.wireguard.crypto.KeyPair
import java.util.ArrayList

class WireGuard {
    val peers = ArrayList<Peer>()
    var listeningPort: Int? = null
    var interfaze: Interface = Interface()
    var raw: String = ""
    var hasError: Boolean = false

    var id: String = ""
    var isActive: Boolean = false
    var isEnabled: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (other !is WireGuard) return false
        return interfaze == other.interfaze && peers == other.peers
    }

    override fun hashCode(): Int {
        return 31 * interfaze.hashCode() + peers.hashCode()
    }

    override fun toString(): String {
        val lines = mutableListOf<String>()
        lines.add(interfaze.toString())
        for (peer in peers) {
            lines.add("")
            lines.add(peer.toString())
        }
        return lines.joinToString("\n")
    }

    fun parse(content: String) {
        raw = content
        val interfaceLines = mutableListOf<String>()
        val peerLines = mutableListOf<String>()
        var inInterfaceSection = false
        var inPeerSection = false
        var seenInterfaceSection = false
        content.split("\n").forEach { l ->
            var line = l
            if (line.startsWith("#app:")) {
                line = line.replace("#app:", "")
            }

            val commentIndex = line.indexOf('#')
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex)
            }
            line = line.trim { it <= ' ' }
            if (line.isEmpty()) {
                return@forEach
            }

            when {
                line.startsWith("[") -> {
                    // Consume all [Peer] lines read so far.
                    if (inPeerSection) {
                        val peer = Peer()
                        peer.parse(peerLines)
                        peers.add(peer)
                        peerLines.clear()
                    }
                    when {
                        "[Interface]".equals(line, ignoreCase = true) -> {
                            inInterfaceSection = true
                            inPeerSection = false
                            seenInterfaceSection = true
                        }
                        "[Peer]".equals(line, ignoreCase = true) -> {
                            inInterfaceSection = false
                            inPeerSection = true
                        }
                        else -> {
                            hasError = true
                        }
                    }
                }
                inInterfaceSection -> {
                    interfaceLines.add(line)
                }
                inPeerSection -> {
                    peerLines.add(line)
                }
                else -> {
                    hasError = true
                }
            }
        }
        if (inPeerSection) {
            val peer = Peer()
            peer.parse(peerLines)
            peers.add(peer)
        }

        if (!seenInterfaceSection) {
            hasError = true
        }

        if (peers.any { it.hasError() }) {
            hasError = true
        }

        interfaze.parse(interfaceLines)
    }

    fun generateNewPeer(): Peer {
        val ip = if (interfaze.addresses.size > 0) interfaze.addresses.first() else ""
        val peer = Peer()
        peer.name = "Peer ${peers.size + 1}"
        peer.keyPair = KeyPair()
        peer.publicKey = peer.keyPair!!.publicKey
        peer.allowedIps.add(ip.substring(0, ip.lastIndexOf(".")) + ".${peers.size + 2}/32")

        return peer
    }

    companion object {
        fun generateId(wgs: List<WireGuard>): String {
            var index = 0
            var id: String
            do {
                id = "wg${index++}"
            } while (wgs.any { it.id == id })

            return id
        }

        fun createNew(): WireGuard {
            val wg = WireGuard()
            wg.interfaze.name = "WireGuard"
            val ip = NetworkHelper.randomFirstIP()
            wg.interfaze.addresses.add(ip)
            val peer = wg.generateNewPeer()
            wg.peers.add(peer)
            wg.raw = wg.toString()
            return wg
        }
    }
}
