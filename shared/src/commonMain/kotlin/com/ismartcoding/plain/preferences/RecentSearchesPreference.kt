package com.ismartcoding.plain.preferences

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.encodeToString

/**
 * LRU list of recent global search terms, capped at [MAX] entries.
 */
object RecentSearchesPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("recent_searches")

    private const val MAX = 10

    suspend fun getValueAsync(): List<String> {
        val str = getAsync()
        if (str.isEmpty()) return listOf()
        return try {
            preferencesJson.decodeFromString<List<String>>(str)
        } catch (_: Exception) {
            listOf()
        }
    }

    /** Moves [term] to the front of the LRU list, capped at [MAX] entries. */
    suspend fun recordAsync(term: String): List<String> {
        val items = getValueAsync().toMutableList()
        items.removeAll { it.equals(term, ignoreCase = true) }
        items.add(0, term)
        while (items.size > MAX) items.removeAt(items.lastIndex)
        putAsync(preferencesJson.encodeToString(items))
        return items
    }

    suspend fun removeAsync(term: String): List<String> {
        val items = getValueAsync().toMutableList()
        items.removeAll { it.equals(term, ignoreCase = true) }
        putAsync(preferencesJson.encodeToString(items))
        return items
    }

    suspend fun clearAsync() {
        putAsync(preferencesJson.encodeToString(emptyList<String>()))
    }
}
