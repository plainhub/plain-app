package com.ismartcoding.plain.preferences

import androidx.datastore.preferences.core.stringPreferencesKey
import com.ismartcoding.plain.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.audio.DPlaylistAudio

// HomeFeaturesPreference and HomeSectionCollapsedPreference have been moved to commonMain.

object AudioPlaylistPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("audio_playlist")

    suspend fun getValueAsync(): List<DPlaylistAudio> {
        val str = getAsync()
        if (str.isEmpty()) return listOf()
        return try { jsonDecode(str) } catch (_: Exception) { listOf() }
    }

    suspend fun putAsync(value: List<DPlaylistAudio>) {
        putAsync(jsonEncode(value))
    }

    suspend fun deleteAsync(paths: Set<String>): List<DPlaylistAudio> {
        val items = getValueAsync().toMutableList().apply { removeAll { paths.contains(it.path) } }
        putAsync(items)
        return items
    }

    suspend fun addAsync(audios: List<DPlaylistAudio>): List<DPlaylistAudio> {
        val items = getValueAsync().toMutableList()
        val paths = audios.map { it.path }
        items.removeAll { paths.contains(it.path) }
        items.addAll(audios)
        putAsync(items)
        return items
    }
}
