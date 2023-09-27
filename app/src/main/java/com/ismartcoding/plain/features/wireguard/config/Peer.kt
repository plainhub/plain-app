package com.ismartcoding.plain.features.wireguard.config

import com.ismartcoding.plain.features.wireguard.config.Attribute.Companion.parse
import com.ismartcoding.plain.features.wireguard.config.Attribute.Companion.split
import com.ismartcoding.plain.features.wireguard.crypto.Key
import com.ismartcoding.plain.features.wireguard.crypto.KeyPair
import java.util.*

class Peer {
    var name: String = ""
    val allowedIps: MutableSet<String> = mutableSetOf()
    var endpoint: String = ""
    var endpointing: String = ""
    var persistentKeepalive: Int? = null
    var preSharedKey: Key? = null
    var keyPair: KeyPair? = null
    var publicKey: Key? = null
    var rxBytes: Long = 0
    var txBytes: Long = 0
    var latestHandshake: Date? = null

    fun getDisplayName(): String {
        if (name.isEmpty()) {
            return publicKey?.toBase64() ?: ""
        }

        return name
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Peer) return false
        return allowedIps == other.allowedIps && endpoint == other.endpoint && persistentKeepalive == other.persistentKeepalive && preSharedKey == other.preSharedKey && publicKey == other.publicKey
    }

    override fun hashCode(): Int {
        var hash = 1
        hash = 31 * hash + allowedIps.hashCode()
        hash = 31 * hash + endpoint.hashCode()
        hash = 31 * hash + persistentKeepalive.hashCode()
        hash = 31 * hash + preSharedKey.hashCode()
        hash = 31 * hash + publicKey.hashCode()
        return hash
    }

    fun hasError(): Boolean {
        if (publicKey == null) {
            return true
        }

        return false
    }

    override fun toString(): String {
        val lines = mutableListOf<String>()
        lines.add("[Peer]")
        if (name.isNotEmpty()) {
            lines.add("#app:Name = $name")
        }
        keyPair?.let {
            lines.add("#app:PrivateKey = ${it.privateKey.toBase64()}")
        }
        if (allowedIps.isNotEmpty()) {
            lines.add("AllowedIPs = ${allowedIps.joinToString(",")}")
        }
        if (endpoint.isNotEmpty()) {
            lines.add("Endpoint = $endpoint")
        }
        persistentKeepalive?.let { lines.add("PersistentKeepalive = $it") }
        preSharedKey?.let { lines.add("PreSharedKey = ${it.toBase64()}") }
        publicKey?.let { lines.add("PublicKey = ${it.toBase64()}") }
        return lines.joinToString("\n")
    }

    fun parse(lines: List<String>) {
        for (line in lines) {
            val attribute = parse(line) ?: continue
            when (attribute.key.lowercase(Locale.ENGLISH)) {
                "name" -> name = attribute.value
                "privatekey" -> keyPair = KeyPair(Key.fromBase64(attribute.value))
                "allowedips" -> allowedIps.addAll(split(attribute.value))
                "endpoint" -> endpoint = attribute.value
                "persistentkeepalive" -> persistentKeepalive = attribute.value.toIntOrNull()
                "presharedkey" -> preSharedKey = Key.fromBase64(attribute.value)
                "publickey" -> publicKey = Key.fromBase64(attribute.value)
            }
        }
        keyPair?.let {
            publicKey = it.publicKey
        }
    }
}
