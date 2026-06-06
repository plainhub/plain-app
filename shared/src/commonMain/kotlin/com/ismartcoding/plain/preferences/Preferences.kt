package com.ismartcoding.plain.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.data.DUpdateInfo
import com.ismartcoding.plain.data.FilePathData
import com.ismartcoding.plain.data.NotificationFilterData
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.enums.PasswordType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val preferencesJson = Json { ignoreUnknownKeys = true }

object PasswordPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("password")
}

object PasswordTypePreference : BasePreference<Int>() {
    override val default = PasswordType.NONE.value
    override val key = intPreferencesKey("password_type")

    suspend fun putAsync(value: PasswordType) {
        putAsync(value.value)
    }

    fun getValue(preferences: Preferences): PasswordType {
        return PasswordType.parse(get(preferences))
    }

    suspend fun getValueAsync(): PasswordType {
        return PasswordType.parse(getAsync())
    }
}

object AuthTwoFactorPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("auth_two_factor")
}

object RotateUrlTokenOnRestartPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("rotate_url_token_on_restart")
}

object AuthDevTokenPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("auth_dev_token")
}

object AdbTokenPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("adb_token")
}

object UpdateInfoPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("update_info")

    fun getValue(preferences: Preferences): DUpdateInfo {
        val str = get(preferences)
        if (str.isEmpty()) return DUpdateInfo()
        return try {
            preferencesJson.decodeFromString(str)
        } catch (_: Exception) {
            DUpdateInfo()
        }
    }

    suspend fun getValueAsync(): DUpdateInfo {
        val str = getAsync()
        if (str.isEmpty()) return DUpdateInfo()
        return try {
            preferencesJson.decodeFromString(str)
        } catch (_: Exception) {
            DUpdateInfo()
        }
    }

    suspend fun putAsync(value: DUpdateInfo) {
        putAsync(preferencesJson.encodeToString(value))
    }

    suspend fun updateAsync(block: (DUpdateInfo) -> DUpdateInfo) {
        putAsync(block(getValueAsync()))
    }
}

object UrlTokenPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("url_token")
}

object ApiPermissionsPreference : BasePreference<Set<String>>() {
    override val default = setOf<String>()
    override val key = stringSetPreferencesKey("api_permissions")
}

object HttpPortPreference : BasePreference<Int>() {
    override val default = 8080
    override val key = intPreferencesKey("http_port")

    override suspend fun putAsync(value: Int) {
        super.putAsync(value)
        TempData.httpPort.value = value
    }
}

object HttpsPortPreference : BasePreference<Int>() {
    override val default = 8443
    override val key = intPreferencesKey("https_port")

    override suspend fun putAsync(value: Int) {
        super.putAsync(value)
        TempData.httpsPort.value = value
    }
}

object DarkThemePreference : BasePreference<Int>() {
    override val default = DarkTheme.UseDeviceTheme.value
    override val key = intPreferencesKey("dark_theme")
}

object AmoledDarkThemePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("amoled_dark_theme")
}

object KeepAwakePreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("keep_awake")
}

object LanguagePreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("locale")
}

object WebPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("web")

    override suspend fun putAsync(value: Boolean) {
        super.putAsync(value)
        TempData.webEnabled.value = value
    }
}

object DeveloperModePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("developer_mode")
}

object DeviceNamePreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("device_name")
}

object HttpsPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("https")

    override suspend fun putAsync(value: Boolean) {
        super.putAsync(value)
        TempData.webHttps.value = value
    }
}

object ScreenMirrorQualityPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("screen_mirror_quality")

    suspend fun getValueAsync(): DScreenMirrorQuality {
        val str = getAsync()
        if (str.isEmpty()) return DScreenMirrorQuality()
        return try {
            preferencesJson.decodeFromString(str)
        } catch (_: Exception) {
            DScreenMirrorQuality()
        }
    }

    suspend fun putAsync(value: DScreenMirrorQuality) {
        putAsync(preferencesJson.encodeToString(value))
    }
}

object ClientIdPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("client_id")
}

object KeyStorePasswordPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("key_store_password")
}

object AudioPlayModePreference : BasePreference<Int>() {
    override val default = MediaPlayMode.REPEAT.ordinal
    override val key = intPreferencesKey("audio_play_mode")

    suspend fun getValueAsync(): MediaPlayMode {
        val value = getAsync()
        return MediaPlayMode.entries.find { it.ordinal == value } ?: MediaPlayMode.REPEAT
    }

    fun getValue(preferences: Preferences): MediaPlayMode {
        val value = preferences[key]
        return MediaPlayMode.entries.find { it.ordinal == value } ?: MediaPlayMode.REPEAT
    }

    suspend fun putAsync(value: MediaPlayMode) {
        super.putAsync(value.ordinal)
        TempData.audioPlayMode.value = value
    }
}

object ImageGridCellsPerRowPreference : BasePreference<Int>() {
    override val default = 3
    override val key = intPreferencesKey("image_grid_cells_per_row")
}

object VideoGridCellsPerRowPreference : BasePreference<Int>() {
    override val default = 3
    override val key = intPreferencesKey("video_grid_cells_per_row")
}

object ShowHiddenFilesPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("show_hidden_files")
}

object NoteEditModePreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("note_edit_mode")
}

object FeedAutoRefreshPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("feed_auto_refresh")
}

object FeedAutoRefreshIntervalPreference : BasePreference<Int>() {
    override val default = 7200
    override val key = intPreferencesKey("feed_auto_refresh_interval")
}

object FeedAutoRefreshOnlyWifiPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("feed_auto_refresh_only_wifi")
}

object EditorAccessoryLevelPreference : BasePreference<Int>() {
    override val default = 0
    override val key = intPreferencesKey("editor_accessory_level")
}

object EditorWrapContentPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("editor_wrap_content")
}

object EditorShowLineNumbersPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("editor_show_line_numbers")
}

object EditorSyntaxHighlightPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("editor_syntax_highlight")
}

object AudioSleepTimerMinutesPreference : BasePreference<Int>() {
    override val default = 30
    override val key = intPreferencesKey("audio_sleep_timer_minutes")
}

object AudioSleepTimerFinishLastPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("audio_sleep_timer_finish_last")
}

object LastFilePathPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("last_file_path")

    suspend fun getValueAsync(): FilePathData {
        val str = getAsync()
        if (str.isEmpty()) return FilePathData("", "", "")
        return try {
            preferencesJson.decodeFromString(str)
        } catch (_: Exception) {
            FilePathData("", "", "")
        }
    }

    suspend fun putAsync(data: FilePathData) {
        putAsync(preferencesJson.encodeToString(data))
    }
}

object FavoriteFoldersPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("favorite_folders")

    suspend fun getValueAsync(): List<DFavoriteFolder> {
        val str = getAsync()
        if (str.isEmpty()) return listOf()
        return try {
            preferencesJson.decodeFromString(str)
        } catch (_: Exception) {
            listOf()
        }
    }

    suspend fun putAsync(value: List<DFavoriteFolder>) {
        putAsync(preferencesJson.encodeToString(value))
    }

    suspend fun addAsync(folder: DFavoriteFolder): List<DFavoriteFolder> {
        val items = getValueAsync().toMutableList()
        items.removeAll { it.fullPath == folder.fullPath }
        items.add(folder)
        putAsync(items)
        return items
    }

    suspend fun removeAsync(fullPath: String): List<DFavoriteFolder> {
        val items = getValueAsync().toMutableList()
        items.removeAll { it.fullPath == fullPath }
        putAsync(items)
        return items
    }

    suspend fun isFavoriteAsync(fullPath: String): Boolean {
        return getValueAsync().any { it.fullPath == fullPath }
    }
}

object ScanHistoryPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("scan_history")

    suspend fun getValueAsync(): List<String> {
        val str = getAsync()
        if (str.isEmpty()) return listOf()
        return try {
            preferencesJson.decodeFromString(str)
        } catch (_: Exception) {
            listOf()
        }
    }

    suspend fun putAsync(value: List<String>) {
        putAsync(preferencesJson.encodeToString(value))
    }
}

object AudioPlayingPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("audio_playing")

    suspend fun getValueAsync(): String {
        val str = getAsync()
        if (str.isEmpty() || str.startsWith("{")) return ""
        return str
    }
}

object ChatInputTextPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("chat_input_text")
}

object NearbyDiscoverablePreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("nearby_discoverable")
}

object MdnsHostnamePreference : BasePreference<String>() {
    override val default = "plainapp.local"
    override val key = stringPreferencesKey("mdns_hostname")
}

object AiImageSearchEnabledPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("ai_image_search_enabled")
}

object DocTabsModePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("doc_tabs_mode")
}

object FidUriExtMigratedPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("fid_uri_ext_migrated")
}

object NotificationFilterPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("notification_filter")

    suspend fun getValueAsync(): NotificationFilterData {
        val str = getAsync()
        if (str.isEmpty()) return NotificationFilterData()
        return try {
            preferencesJson.decodeFromString(str)
        } catch (_: Exception) {
            NotificationFilterData()
        }
    }

    suspend fun putAsync(data: NotificationFilterData) {
        putAsync(preferencesJson.encodeToString(data))
    }

    suspend fun toggleAppAsync(packageName: String) {
        val data = getValueAsync()
        val newApps = data.apps.toMutableSet()
        if (newApps.contains(packageName)) newApps.remove(packageName) else newApps.add(packageName)
        putAsync(data.copy(apps = newApps))
    }

    suspend fun setModeAsync(mode: String) {
        val data = getValueAsync()
        putAsync(data.copy(mode = mode))
    }

    suspend fun isAllowedAsync(packageName: String): Boolean {
        val data = getValueAsync()
        return when (data.mode) {
            "allowlist" -> data.apps.contains(packageName)
            "blacklist" -> !data.apps.contains(packageName)
            else -> true
        }
    }
}

object PomodoroSettingsPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("pomodoro_settings")

    suspend fun getValueAsync(): DPomodoroSettings {
        val str = getAsync()
        if (str.isEmpty()) return DPomodoroSettings()
        return try {
            preferencesJson.decodeFromString(str)
        } catch (_: Exception) {
            DPomodoroSettings()
        }
    }

    suspend fun putAsync(value: DPomodoroSettings) {
        putAsync(preferencesJson.encodeToString(value))
    }
}

object SignatureKeyPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("signature_key_pair")
}

object VideoPlaylistPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("video_playlist")

    suspend fun getValueAsync(): List<DVideo> {
        val str = getAsync()
        if (str.isEmpty()) return listOf()
        return try {
            preferencesJson.decodeFromString(str)
        } catch (_: Exception) {
            listOf()
        }
    }

    suspend fun putAsync(value: List<DVideo>) {
        putAsync(preferencesJson.encodeToString(value))
    }

    suspend fun deleteAsync(paths: Set<String>) {
        putAsync(getValueAsync().toMutableList().apply { removeAll { paths.contains(it.path) } })
    }

    suspend fun addAsync(videos: List<DVideo>) {
        val items = getValueAsync().toMutableList()
        items.removeAll { i -> videos.any { it.path == i.path } }
        items.addAll(videos)
        putAsync(items)
    }
}

object DlnaAllowedSendersPreference : BasePreference<Set<String>>() {
    override val default = setOf<String>()
    override val key = stringSetPreferencesKey("dlna_allowed_senders")

    suspend fun addAsync(ip: String, name: String) {
        val current = getAsync().toMutableSet()
        current.removeAll { decodeSenderEntry(it).first == ip }
        current.add(encodeSenderEntry(ip, name))
        putAsync(current)
    }

    suspend fun removeAsync(ip: String) {
        val current = getAsync().toMutableSet()
        current.removeAll { decodeSenderEntry(it).first == ip }
        putAsync(current)
    }

    fun containsIp(entries: Set<String>, ip: String) = entries.any { decodeSenderEntry(it).first == ip }
}

object DlnaDeniedSendersPreference : BasePreference<Set<String>>() {
    override val default = setOf<String>()
    override val key = stringSetPreferencesKey("dlna_denied_senders")

    suspend fun addAsync(ip: String, name: String) {
        val current = getAsync().toMutableSet()
        current.removeAll { decodeSenderEntry(it).first == ip }
        current.add(encodeSenderEntry(ip, name))
        putAsync(current)
    }

    suspend fun removeAsync(ip: String) {
        val current = getAsync().toMutableSet()
        current.removeAll { decodeSenderEntry(it).first == ip }
        putAsync(current)
    }

    fun containsIp(entries: Set<String>, ip: String) = entries.any { decodeSenderEntry(it).first == ip }
}

private const val DLNA_SEP = "|"

fun encodeSenderEntry(ip: String, name: String) = "$ip$DLNA_SEP$name"

fun decodeSenderEntry(entry: String): Pair<String, String> {
    val idx = entry.indexOf(DLNA_SEP)
    return if (idx >= 0) entry.substring(0, idx) to entry.substring(idx + 1)
    else entry to ""
}
