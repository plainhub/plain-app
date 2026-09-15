package com.ismartcoding.plain.preferences

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.encodeToString

/**
 * LRU list of recently used save directories (custom destinations picked in
 * save sheets), capped at [MAX] entries.
 */
object RecentSaveDirsPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("recent_save_dirs")

    private const val MAX = 5

    suspend fun getValueAsync(): List<String> {
        val str = getAsync()
        if (str.isEmpty()) return listOf()
        return try {
            preferencesJson.decodeFromString<List<String>>(str)
        } catch (_: Exception) {
            listOf()
        }
    }

    /** Moves [dir] to the front of the LRU list, capped at [MAX] entries. */
    suspend fun recordAsync(dir: String): List<String> {
        val items = getValueAsync().toMutableList()
        items.removeAll { it == dir }
        items.add(0, dir)
        while (items.size > MAX) items.removeAt(items.lastIndex)
        putAsync(preferencesJson.encodeToString(items))
        return items
    }
}
