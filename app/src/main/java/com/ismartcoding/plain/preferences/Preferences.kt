package com.ismartcoding.plain.preferences

import android.content.Context
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.data.NotificationFilterData
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.data.FilePathData
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.Language
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import java.util.Locale


object PasswordPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("password")
}

object PasswordTypePreference : BasePreference<Int>() {
    override val default = PasswordType.NONE.value
    override val key = intPreferencesKey("password_type")

    suspend fun putAsync(
        context: Context,
        value: PasswordType,
    ) {
        putAsync(context, value.value)
    }

    fun getValue(preferences: Preferences): PasswordType {
        return PasswordType.parse(get(preferences))
    }

    suspend fun getValueAsync(context: Context): PasswordType {
        return PasswordType.parse(getAsync(context))
    }
}

object AuthTwoFactorPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferencesKey("auth_two_factor")
}

object AuthDevTokenPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("auth_dev_token")
}

object NewVersionPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("new_version")
}

object CheckUpdateTimePreference : BasePreference<Long>() {
    override val default = 0L
    override val key = longPreferencesKey("check_update_time")
}

object SkipVersionPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("skip_version")
}

object NewVersionPublishDatePreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("new_version_publish_date")
}

object NewVersionLogPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("new_version_log")
}

object NewVersionDownloadUrlPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("new_version_download_url")
}

object NewVersionSizePreference : BasePreference<Long>() {
    override val default = 0L
    override val key = longPreferencesKey("new_version_size")
}

object UrlTokenPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("url_token")

    suspend fun ensureValueAsync(
        context: Context,
        preferences: Preferences,
    ) {
        TempData.urlToken = get(preferences)
        if (TempData.urlToken.isEmpty()) {
            TempData.urlToken = CryptoHelper.generateChaCha20Key()
            putAsync(context, TempData.urlToken)
        }
    }

    suspend fun resetAsync(context: Context) {
        TempData.urlToken = CryptoHelper.generateChaCha20Key()
        putAsync(context, TempData.urlToken)
    }
}

object ApiPermissionsPreference : BasePreference<Set<String>>() {
    override val default = setOf<String>()
    override val key = stringSetPreferencesKey("api_permissions")

    suspend fun putAsync(
        context: Context,
        permission: Permission,
        enable: Boolean,
    ) {
        val permissions = getAsync(context).toMutableSet()
        if (enable) {
            permissions.add(permission.name)
        } else {
            permissions.remove(permission.name)
        }
        putAsync(context, permissions)
    }
}

object HttpPortPreference : BasePreference<Int>() {
    override val default = 8080
    override val key = intPreferencesKey("http_port")
}

object HttpsPortPreference : BasePreference<Int>() {
    override val default = 8443
    override val key = intPreferencesKey("https_port")
}

object DarkThemePreference : BasePreference<Int>() {
    override val default = DarkTheme.UseDeviceTheme.value
    override val key = intPreferencesKey("dark_theme")

    suspend fun putAsync(
        context: Context,
        value: DarkTheme,
    ) {
        putAsync(context, value.value)
        setDarkMode(value)
    }

    fun setDarkMode(theme: DarkTheme) {
        when (theme) {
            DarkTheme.ON -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            DarkTheme.OFF -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}

object CustomPrimaryColorPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("custom_primary_color")
}

object AmoledDarkThemePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("amoled_dark_theme")
}

object ThemeIndexPreference : BasePreference<Int>() {
    override val default = 5
    override val key = intPreferencesKey("theme_index")
}

object KeepScreenOnPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("keep_screen_on")
}

object KeepAwakePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("keep_awake")
}

object SystemScreenTimeoutPreference : BasePreference<Int>() {
    override val default = 0
    override val key = intPreferencesKey("system_screen_timeout")
}

object LanguagePreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("locale")

    suspend fun getLocaleAsync(context: Context): Locale? {
        return getLocale(getAsync(context))
    }

    fun getLocale(preferences: Preferences): Locale? {
        return getLocale(get(preferences))
    }

    private fun getLocale(value: String): Locale? {
        if (value.isEmpty()) {
            return null
        }

        val s = value.split("-")
        return if (s.size > 1) {
            Locale(s[0], s[1])
        } else {
            Locale(value)
        }
    }

    suspend fun putAsync(
        context: Context,
        locale: Locale?,
    ) {
        var value = ""
        if (locale != null) {
            value = locale.language
            if (locale.country.isNotEmpty()) {
                value += "-${locale.country}"
            }
        }
        putAsync(context, value)
        Language.setLocale(context, locale ?: LocaleList.getDefault().get(0))
    }
}

object WebPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("web")

    override suspend fun putAsync(
        context: Context,
        value: Boolean,
    ) {
        TempData.webEnabled = value
        super.putAsync(context, value)
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

    override suspend fun putAsync(
        context: Context,
        value: Boolean,
    ) {
        TempData.webHttps = value
        super.putAsync(context, value)
    }
}

object ScreenMirrorQualityPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("screen_mirror_quality")

    suspend fun getValueAsync(context: Context): DScreenMirrorQuality {
        val str = getAsync(context)
        if (str.isEmpty()) {
            return DScreenMirrorQuality()
        }
        return jsonDecode(str)
    }

    suspend fun putAsync(
        context: Context,
        value: DScreenMirrorQuality,
    ) {
        putAsync(context, jsonEncode(value))
    }
}

object ClientIdPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("client_id")

    suspend fun ensureValueAsync(
        context: Context,
        preferences: Preferences,
    ) {
        TempData.clientId = get(preferences)
        if (TempData.clientId.isEmpty()) {
            TempData.clientId = StringHelper.shortUUID()
            putAsync(context, TempData.clientId)
        }
    }
}

object KeyStorePasswordPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("key_store_password")

    suspend fun ensureValueAsync(
        context: Context,
        preferences: Preferences,
    ) {
        var password = get(preferences)
        if (password.isEmpty()) {
            password = StringHelper.shortUUID()
            putAsync(context, password)
        }
    }

    suspend fun resetAsync(context: Context) {
        putAsync(context, StringHelper.shortUUID())
    }
}

object AudioPlayModePreference : BasePreference<Int>() {
    override val default = MediaPlayMode.REPEAT.ordinal
    override val key = intPreferencesKey("audio_play_mode")

    suspend fun putAsync(
        context: Context,
        value: MediaPlayMode,
    ) {
        putAsync(context, value.ordinal)
        TempData.audioPlayMode = value
    }

    suspend fun getValueAsync(context: Context): MediaPlayMode {
        val value = getAsync(context)
        return MediaPlayMode.entries.find { it.ordinal == value } ?: MediaPlayMode.REPEAT
    }

    fun getValue(preferences: Preferences): MediaPlayMode {
        val value = preferences[key]
        return MediaPlayMode.entries.find { it.ordinal == value } ?: MediaPlayMode.REPEAT
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

abstract class BaseSortByPreference(
    val prefix: String,
    private val defaultSort: FileSortBy = FileSortBy.DATE_DESC
) : BasePreference<Int>() {
    override val default = defaultSort.ordinal
    override val key = intPreferencesKey("${prefix}_sort_by")

    suspend fun putAsync(
        context: Context,
        value: FileSortBy,
    ) {
        putAsync(context, value.ordinal)
    }

    suspend fun getValueAsync(context: Context): FileSortBy {
        val value = getAsync(context)
        return FileSortBy.entries.find { it.ordinal == value } ?: defaultSort
    }
}

object AudioSortByPreference : BaseSortByPreference("audio")
object VideoSortByPreference : BaseSortByPreference("video")
object ImageSortByPreference : BaseSortByPreference("image")
object DocSortByPreference : BaseSortByPreference("doc")
object FileSortByPreference : BaseSortByPreference("file", FileSortBy.NAME_ASC)
object PackageSortByPreference : BaseSortByPreference("pkg", FileSortBy.NAME_ASC)

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

    suspend fun getValueAsync(context: Context): FilePathData {
        val str = getAsync(context)
        if (str.isEmpty()) {
            return FilePathData("", "", "")
        }
        return try {
            jsonDecode(str)
        } catch (e: Exception) {
            // If JSON parsing fails, return empty data
            FilePathData("", "", "")
        }
    }

    suspend fun putAsync(
        context: Context,
        data: FilePathData
    ) {
        putAsync(context, jsonEncode(data))
    }
}

object FavoriteFoldersPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("favorite_folders")

    suspend fun getValueAsync(context: Context): List<DFavoriteFolder> {
        val str = getAsync(context)
        if (str.isEmpty()) {
            return listOf()
        }
        return try {
            jsonDecode(str)
        } catch (e: Exception) {
            listOf()
        }
    }

    suspend fun putAsync(
        context: Context,
        value: List<DFavoriteFolder>
    ) {
        putAsync(context, jsonEncode(value))
    }

    suspend fun addAsync(
        context: Context,
        folder: DFavoriteFolder
    ): List<DFavoriteFolder> {
        val items = getValueAsync(context).toMutableList()
        items.removeIf { it.fullPath == folder.fullPath }
        items.add(folder)
        putAsync(context, items)
        return items
    }

    suspend fun removeAsync(
        context: Context,
        fullPath: String
    ): List<DFavoriteFolder> {
        val items = getValueAsync(context).toMutableList()
        items.removeIf { it.fullPath == fullPath }
        putAsync(context, items)
        return items
    }

    suspend fun isFavoriteAsync(
        context: Context,
        fullPath: String
    ): Boolean {
        return getValueAsync(context).any { it.fullPath == fullPath }
    }
}

object ScanHistoryPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("scan_history")

    suspend fun getValueAsync(context: Context): List<String> {
        val str = getAsync(context)
        if (str.isEmpty()) {
            return listOf()
        }
        return jsonDecode(str)
    }

    suspend fun putAsync(
        context: Context,
        value: List<String>,
    ) {
        putAsync(context, jsonEncode(value))
    }
}

object AudioPlaylistPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("audio_playlist")

    suspend fun getValueAsync(context: Context): List<DPlaylistAudio> {
        val str = getAsync(context)
        if (str.isEmpty()) {
            return listOf()
        }
        return jsonDecode(str)
    }

    suspend fun putAsync(
        context: Context,
        value: List<DPlaylistAudio>,
    ) {
        putAsync(context, jsonEncode(value))
    }

    suspend fun deleteAsync(
        context: Context,
        paths: Set<String>,
    ): List<DPlaylistAudio> {
        val items = getValueAsync(context).toMutableList().apply {
            removeIf { paths.contains(it.path) }
        }
        putAsync(context, items)
        return items
    }

    suspend fun addAsync(
        context: Context,
        audios: List<DPlaylistAudio>,
    ): List<DPlaylistAudio> {
        val items = getValueAsync(context).toMutableList()
        val paths = audios.map { it.path }
        items.removeIf { paths.contains(it.path) }
        items.addAll(audios)
        putAsync(context, items)
        return items
    }
}

object AudioPlayingPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("audio_playing")

    suspend fun getValueAsync(context: Context): String {
        val str = getAsync(context)
        if (str.isEmpty() || str.startsWith("{")) {
            return ""
        }
        return str
    }
}

object ChatInputTextPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("chat_input_text")
}

object ChatFilesSaveFolderPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("chat_files_save_dir")
}

object VideoPlaylistPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("video_playlist")

    suspend fun getValueAsync(context: Context): List<DVideo> {
        val str = getAsync(context)
        if (str.isEmpty()) {
            return listOf()
        }
        return jsonDecode(str)
    }

    suspend fun putAsync(
        context: Context,
        value: List<DVideo>,
    ) {
        putAsync(context, jsonEncode(value))
    }

    suspend fun deleteAsync(
        context: Context,
        paths: Set<String>,
    ) {
        putAsync(
            context,
            getValueAsync(context).toMutableList().apply {
                removeIf { paths.contains(it.path) }
            },
        )
    }

    suspend fun addAsync(
        context: Context,
        videos: List<DVideo>,
    ) {
        val items = getValueAsync(context).toMutableList()
        items.removeIf { i -> videos.any { it.path == i.path } }
        items.addAll(videos)
        putAsync(context, items)
    }
}

object HomeFeaturesPreference : BasePreference<Set<String>>() {
    override val default = setOf(
        AppFeatureType.FILES, AppFeatureType.DOCS, AppFeatureType.APPS, AppFeatureType.NOTES, AppFeatureType.FEEDS
    ).map { it.name }.toSet()
    override val key = stringSetPreferencesKey("home_features")
}

object NotificationFilterPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("notification_filter")

    suspend fun getValueAsync(context: Context): NotificationFilterData {
        val str = getAsync(context)
        if (str.isEmpty()) {
            return NotificationFilterData()
        }
        return try {
            jsonDecode(str)
        } catch (e: Exception) {
            NotificationFilterData()
        }
    }

    suspend fun putAsync(
        context: Context,
        data: NotificationFilterData
    ) {
        putAsync(context, jsonEncode(data))
    }

    suspend fun toggleAppAsync(
        context: Context,
        packageName: String,
    ) {
        val data = getValueAsync(context)
        val newApps = data.apps.toMutableSet()
        if (newApps.contains(packageName)) {
            newApps.remove(packageName)
        } else {
            newApps.add(packageName)
        }
        putAsync(context, data.copy(apps = newApps))
    }

    suspend fun setModeAsync(
        context: Context,
        mode: String
    ) {
        val data = getValueAsync(context)
        putAsync(context, data.copy(mode = mode))
    }

    suspend fun isAllowedAsync(context: Context, packageName: String): Boolean {
        val data = getValueAsync(context)
        return when (data.mode) {
            "allowlist" -> {
                data.apps.contains(packageName)
            }
            "blacklist" -> {
                !data.apps.contains(packageName)
            }
            else -> true
        }
    }
}

object PomodoroSettingsPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("pomodoro_settings")

    suspend fun getValueAsync(context: Context): DPomodoroSettings {
        val str = getAsync(context)
        if (str.isEmpty()) {
            return DPomodoroSettings()
        }
        return try {
            jsonDecode(str)
        } catch (e: Exception) {
            DPomodoroSettings()
        }
    }

    suspend fun putAsync(
        context: Context,
        value: DPomodoroSettings,
    ) {
        putAsync(context, jsonEncode(value))
    }
}

object NearbyDiscoverablePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferencesKey("nearby_discoverable")
}

object MdnsHostnamePreference : BasePreference<String>() {
    override val default = "plainapp.local"
    override val key = stringPreferencesKey("mdns_hostname")

    suspend fun ensureValueAsync(
        preferences: Preferences,
    ) {
        TempData.mdnsHostname = get(preferences).ifEmpty { default }
    }
}
