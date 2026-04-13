package com.ismartcoding.plain.preferences

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * Stores sender device entries as "ip|name" encoded strings.
 * The name part may be empty if unknown.
 */
private const val SEP = "|"

fun encodeSenderEntry(ip: String, name: String) = "$ip$SEP$name"

fun decodeSenderEntry(entry: String): Pair<String, String> {
    val idx = entry.indexOf(SEP)
    return if (idx >= 0) entry.substring(0, idx) to entry.substring(idx + 1)
    else entry to ""
}

object DlnaAllowedSendersPreference : BasePreference<Set<String>>() {
    override val default = setOf<String>()
    override val key = stringSetPreferencesKey("dlna_allowed_senders")

    suspend fun addAsync(context: Context, ip: String, name: String) {
        val current = getAsync(context).toMutableSet()
        // Remove any existing entry for this IP first
        current.removeAll { decodeSenderEntry(it).first == ip }
        current.add(encodeSenderEntry(ip, name))
        putAsync(context, current)
    }

    suspend fun removeAsync(context: Context, ip: String) {
        val current = getAsync(context).toMutableSet()
        current.removeAll { decodeSenderEntry(it).first == ip }
        putAsync(context, current)
    }

    fun containsIp(entries: Set<String>, ip: String) = entries.any { decodeSenderEntry(it).first == ip }
}

object DlnaDeniedSendersPreference : BasePreference<Set<String>>() {
    override val default = setOf<String>()
    override val key = stringSetPreferencesKey("dlna_denied_senders")

    suspend fun addAsync(context: Context, ip: String, name: String) {
        val current = getAsync(context).toMutableSet()
        current.removeAll { decodeSenderEntry(it).first == ip }
        current.add(encodeSenderEntry(ip, name))
        putAsync(context, current)
    }

    suspend fun removeAsync(context: Context, ip: String) {
        val current = getAsync(context).toMutableSet()
        current.removeAll { decodeSenderEntry(it).first == ip }
        putAsync(context, current)
    }

    fun containsIp(entries: Set<String>, ip: String) = entries.any { decodeSenderEntry(it).first == ip }
}
