package com.ismartcoding.plain.features.sms

internal class SmsProviderChangeBuffer {
    private val pendingUris = linkedSetOf<String>()
    private var active = false

    fun start() {
        active = true
    }

    fun add(uri: String): Boolean {
        if (!active) return false
        pendingUris.add(uri)
        return true
    }

    fun drain(): List<String> {
        if (!active) return emptyList()
        return pendingUris.toList().also { pendingUris.clear() }
    }

    fun stop() {
        active = false
        pendingUris.clear()
    }
}
