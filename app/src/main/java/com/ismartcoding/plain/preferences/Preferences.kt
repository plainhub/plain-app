package com.ismartcoding.plain.preferences

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ismartcoding.plain.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.file.FileSortBy

// ── Sort-by preferences (FileSortBy depends on android.provider.MediaStore) ──

abstract class BaseSortByPreference(
    val prefix: String,
    private val defaultSort: FileSortBy = FileSortBy.DATE_DESC
) : BasePreference<Int>() {
    override val default = defaultSort.ordinal
    override val key = intPreferencesKey("${prefix}_sort_by")

    suspend fun putAsync(value: FileSortBy) {
        putAsync(value.ordinal)
    }

    suspend fun getValueAsync(): FileSortBy {
        val value = getAsync()
        return FileSortBy.entries.find { it.ordinal == value } ?: defaultSort
    }
}

object AudioSortByPreference : BaseSortByPreference("audio", FileSortBy.DATE_DESC)
object VideoSortByPreference : BaseSortByPreference("video", FileSortBy.TAKEN_AT_DESC)
object ImageSortByPreference : BaseSortByPreference("image", FileSortBy.TAKEN_AT_DESC)
object DocSortByPreference : BaseSortByPreference("doc")
object FileSortByPreference : BaseSortByPreference("file", FileSortBy.NAME_ASC)
object PackageSortByPreference : BaseSortByPreference("pkg", FileSortBy.NAME_ASC)

// ── AudioPlaylistPreference (DPlaylistAudio depends on Parcelable) ────────────

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

// ── HomeFeaturesPreference (AppFeatureType is Android-specific) ───────────────

object HomeFeaturesPreference : BasePreference<String>() {
    private const val SEPARATOR = "|"
    override val default = listOf(
        AppFeatureType.IMAGES, AppFeatureType.VIDEOS, AppFeatureType.AUDIO,
        AppFeatureType.DOCS, AppFeatureType.FILES, AppFeatureType.CHAT,
    ).joinToString(SEPARATOR) { it.name }
    override val key = stringPreferencesKey("home_features_v2")

    fun parseList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR).filter { it.isNotBlank() }

    fun formatList(list: List<String>): String = list.joinToString(SEPARATOR)
}

// ── HomeSectionCollapsedPreference ────────────────────────────────────────────

object HomeSectionCollapsedPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("home_section_collapsed")

    fun get(preferences: androidx.datastore.preferences.core.Preferences, feature: AppFeatureType): Boolean {
        return parseMap(get(preferences))[feature] ?: false
    }

    suspend fun putAsync(feature: AppFeatureType, collapsed: Boolean) {
        val updated = getValueAsync().toMutableMap()
        updated[feature] = collapsed
        putAsync(formatMap(updated))
    }

    suspend fun getValueAsync(): Map<AppFeatureType, Boolean> {
        return parseMap(getAsync())
    }

    private fun parseMap(value: String): Map<AppFeatureType, Boolean> {
        if (value.isEmpty()) return emptyMap()
        return try {
            jsonDecode<Map<String, Boolean>>(value).mapNotNull { (key, collapsed) ->
                runCatching { AppFeatureType.valueOf(key) }.getOrNull()?.let { it to collapsed }
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun formatMap(value: Map<AppFeatureType, Boolean>): String {
        return jsonEncode(value.mapKeys { it.key.name })
    }
}
