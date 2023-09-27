package com.ismartcoding.plain.features.wireguard.config

import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.features.wireguard.config.Attribute.Companion.parse
import com.ismartcoding.plain.features.wireguard.config.Attribute.Companion.split
import com.ismartcoding.plain.features.wireguard.crypto.Key
import com.ismartcoding.plain.features.wireguard.crypto.KeyPair
import java.util.*

class Interface {
    var name: String = ""
    val addresses: MutableSet<String> = mutableSetOf()
    val dnsServers: MutableSet<String> = mutableSetOf()
    var keyPair: KeyPair = KeyPair()
    var listenPort: Int? = null
    var mtu: Int? = null

    override fun equals(other: Any?): Boolean {
        if (other !is Interface) return false
        return addresses == other.addresses && dnsServers == other.dnsServers && keyPair == other.keyPair && listenPort == other.listenPort && mtu == other.mtu
    }

    override fun hashCode(): Int {
        var hash = 1
        hash = 31 * hash + addresses.hashCode()
        hash = 31 * hash + dnsServers.hashCode()
        hash = 31 * hash + keyPair.hashCode()
        hash = 31 * hash + listenPort.hashCode()
        hash = 31 * hash + mtu.hashCode()
        return hash
    }

    override fun toString(): String {
        val lines = mutableListOf<String>()
        lines.add("[Interface]")
        if (name.isNotEmpty()) {
            lines.add("#app:Name = $name")
        }
        lines.add("PrivateKey = ${keyPair.privateKey.toBase64()}")
        if (addresses.isNotEmpty()) {
            lines.add("Address = ${addresses.joinToString(",")}")
        }
        if (dnsServers.isNotEmpty()) {
            lines.add("DNS = ${dnsServers.joinToString(",")}")
        }
        listenPort?.let { lines.add("ListenPort = $it") }
        mtu?.let { lines.add("MTU = $it") }
        lines.add("Table = off")
        return lines.joinToString("\n")
    }

    fun parse(lines: List<String>) {
        for (line in lines) {
            val attribute = parse(line) ?: continue
            when (attribute.key.lowercase(Locale.ENGLISH)) {
                "name" -> name = attribute.value
                "address" -> addresses.addAll(split(attribute.value))
                "dns" -> dnsServers.addAll(split(attribute.value))
                "listenport" -> listenPort = if (NetworkHelper.isPort(attribute.value)) attribute.value.toInt() else null
                "mtu" -> mtu = attribute.value.toIntOrNull()
                "privatekey" -> keyPair = KeyPair(Key.fromBase64(attribute.value))
            }
        }
    }
}
