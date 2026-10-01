package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.data.DSignatureKeyPair
import com.ismartcoding.plain.data.DUpdateInfo
import com.ismartcoding.plain.data.FilePathData
import com.ismartcoding.plain.data.NotificationFilterData
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper.jsonDecode
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.generateEd25519KeyPair
import com.ismartcoding.plain.platform.randomPassword
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal val preferencesJson = Json { ignoreUnknownKeys = true }

object PasswordPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("password")
}

object PasswordTypePreference : BasePreference<Int>() {
    override val default = PasswordType.NONE.value
    override val key = intPreferenceKey("password_type")

    suspend fun putAsync(value: PasswordType) {
        putAsync(value.value)
    }

    fun getValue(preferences: PreferenceSnapshot): PasswordType {
        return PasswordType.parse(get(preferences))
    }

    suspend fun getValueAsync(): PasswordType {
        return PasswordType.parse(getAsync())
    }
}

object AuthTwoFactorPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferenceKey("auth_two_factor")
}

object RotateUrlTokenOnRestartPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("rotate_url_token_on_restart")
}

object AuthDevTokenPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("auth_dev_token")
}

object AdbTokenPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("adb_token")
}

suspend fun AdbTokenPreference.resetAsync() {
    TempData.adbToken = randomPassword(32)
    putAsync(TempData.adbToken)
}

object UpdateInfoPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("update_info")

    fun getValue(preferences: PreferenceSnapshot): DUpdateInfo {
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
    override val key = stringPreferenceKey("url_token")

    suspend fun ensureValueAsync(preferences: PreferenceSnapshot) {
        val rotateOnRestart = RotateUrlTokenOnRestartPreference.get(preferences)
        if (rotateOnRestart) {
            val keyStr = com.ismartcoding.plain.platform.generateChaCha20Key()
            TempData.urlToken = com.ismartcoding.plain.helpers.Base64Lenient.decode(keyStr)
            putAsync(keyStr)
            return
        }
        val keyStr = get(preferences)
        if (keyStr.isEmpty()) {
            val newKeyStr = com.ismartcoding.plain.platform.generateChaCha20Key()
            TempData.urlToken = com.ismartcoding.plain.helpers.Base64Lenient.decode(newKeyStr)
            putAsync(newKeyStr)
        } else {
            TempData.urlToken = com.ismartcoding.plain.helpers.Base64Lenient.decode(keyStr)
        }
    }

    suspend fun resetAsync() {
        val keyStr = com.ismartcoding.plain.platform.generateChaCha20Key()
        TempData.urlToken = com.ismartcoding.plain.helpers.Base64Lenient.decode(keyStr)
        putAsync(keyStr)
    }

}

object MasterSecretPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("master_secret")

    /** Returns the per-install 32-byte master secret (base64), generating and persisting it on first use. */
    suspend fun ensureValueAsync(): String {
        getAsync().takeIf { it.isNotEmpty() }?.let { return it }
        val generated = com.ismartcoding.plain.platform.generateChaCha20Key()
        putAsync(generated)
        return generated
    }
}

object ApiPermissionsPreference : BasePreference<Set<String>>() {
    override val default = setOf<String>()
    override val key = stringSetPreferenceKey("api_permissions")

    suspend fun putAsync(permission: Permission, enable: Boolean) {
        val permissions = getAsync().toMutableSet()
        if (enable) permissions.add(permission.name) else permissions.remove(permission.name)
        putAsync(permissions)
    }
}

object HttpPortPreference : BasePreference<Int>() {
    override val default = 8080
    override val key = intPreferenceKey("http_port")

    override suspend fun putAsync(value: Int) {
        super.putAsync(value)
        TempData.httpPort.value = value
    }
}

object HttpsPortPreference : BasePreference<Int>() {
    override val default = 8443
    override val key = intPreferenceKey("https_port")

    override suspend fun putAsync(value: Int) {
        super.putAsync(value)
        TempData.httpsPort.value = value
    }
}

object DarkThemePreference : BasePreference<Int>() {
    override val default = DarkTheme.UseDeviceTheme.value
    override val key = intPreferenceKey("dark_theme")
}

object AmoledDarkThemePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("amoled_dark_theme")
}

object PdfFollowDarkThemePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("pdf_follow_dark_theme")
}

object KeepAwakePreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferenceKey("keep_awake")
}

object LanguagePreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("locale")
}

object ServicePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("service")

    override suspend fun putAsync(value: Boolean) {
        super.putAsync(value)
        TempData.serviceEnabled.value = value
    }
}

object OnboardingPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("onboarding_completed")
}

object DesktopAccessPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferenceKey("desktop_access")

    override suspend fun putAsync(value: Boolean) {
        super.putAsync(value)
        TempData.desktopAccessEnabled.value = value
    }
}

object DlnaPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("dlna")

    override suspend fun putAsync(value: Boolean) {
        super.putAsync(value)
        TempData.dlnaEnabled.value = value
    }
}

object DeveloperModePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("developer_mode")
}

object AllowAnyHostPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("allow_any_host")

    override suspend fun putAsync(value: Boolean) {
        super.putAsync(value)
        TempData.allowAnyHost.value = value
    }
}

object DeviceNamePreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("device_name")
}

object HttpsPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("https")

    override suspend fun putAsync(value: Boolean) {
        super.putAsync(value)
        TempData.webHttps.value = value
    }
}

object WebAddressBarExpandedPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("web_address_bar_expanded")

    override suspend fun putAsync(value: Boolean) {
        super.putAsync(value)
        TempData.webAddressBarExpanded.value = value
    }
}

object ScreenMirrorQualityPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("screen_mirror_quality")

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
    override val key = stringPreferenceKey("client_id")

    suspend fun ensureValueAsync(preferences: PreferenceSnapshot) {
        TempData.clientId = get(preferences)
        if (TempData.clientId.isEmpty()) {
            TempData.clientId = StringHelper.shortUUID()
            putAsync(TempData.clientId)
        }
    }
}

object KeyStorePasswordPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("key_store_password")

    suspend fun ensureValueAsync(preferences: PreferenceSnapshot) {
        var password = get(preferences)
        if (password.isEmpty()) {
            password = StringHelper.shortUUID()
            putAsync(password)
        }
    }

    suspend fun resetAsync() {
        putAsync(StringHelper.shortUUID())
    }
}

object AudioPlayModePreference : BasePreference<Int>() {
    override val default = MediaPlayMode.REPEAT.ordinal
    override val key = intPreferenceKey("audio_play_mode")

    suspend fun getValueAsync(): MediaPlayMode {
        val value = getAsync()
        return MediaPlayMode.entries.find { it.ordinal == value } ?: MediaPlayMode.REPEAT
    }

    fun getValue(preferences: PreferenceSnapshot): MediaPlayMode {
        val value = preferences[key]
        return MediaPlayMode.entries.find { it.ordinal == value } ?: MediaPlayMode.REPEAT
    }

    suspend fun putAsync(value: MediaPlayMode) {
        super.putAsync(value.ordinal)
        TempData.audioPlayMode.value = value
    }
}

object AudioPlaybackSpeedPreference : BasePreference<Float>() {
    override val default = 1f
    override val key = floatPreferenceKey("audio_playback_speed")

    fun getValue(preferences: PreferenceSnapshot): Float = preferences[key] ?: default

    override suspend fun putAsync(value: Float) {
        super.putAsync(value)
        TempData.audioPlaybackSpeed.value = value
    }
}

object ImageGridCellsPerRowPreference : BasePreference<Int>() {
    override val default = 3
    override val key = intPreferenceKey("image_grid_cells_per_row")
}

object VideoGridCellsPerRowPreference : BasePreference<Int>() {
    override val default = 3
    override val key = intPreferenceKey("video_grid_cells_per_row")
}

object ShowHiddenFilesPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("show_hidden_files")
}

object NoteEditModePreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferenceKey("note_edit_mode")
}

object FeedAutoRefreshPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferenceKey("feed_auto_refresh")
}

object FeedAutoRefreshIntervalPreference : BasePreference<Int>() {
    override val default = 7200
    override val key = intPreferenceKey("feed_auto_refresh_interval")
}

object FeedAutoRefreshOnlyWifiPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("feed_auto_refresh_only_wifi")
}

object FeedFontScalePreference : BasePreference<Int>() {
    override val default = 1
    override val key = intPreferenceKey("feed_font_scale")
}

object EditorWrapContentPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferenceKey("editor_wrap_content")
}

object EditorFontSizePreference : BasePreference<Int>() {
    override val default = 14
    override val key = intPreferenceKey("editor_font_size")
}

object EditorStatusBarPreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferenceKey("editor_status_bar")
}

object AudioSleepTimerMinutesPreference : BasePreference<Int>() {
    override val default = 30
    override val key = intPreferenceKey("audio_sleep_timer_minutes")
}

object AudioSleepTimerFinishLastPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("audio_sleep_timer_finish_last")
}

object LastFilePathPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("last_file_path")

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
    override val key = stringPreferenceKey("favorite_folders")

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

    /** Rename a favorite folder entry by updating its display alias. */
    suspend fun renameAsync(fullPath: String, alias: String): List<DFavoriteFolder> {
        val items = getValueAsync().toMutableList()
        val index = items.indexOfFirst { it.fullPath == fullPath }
        if (index >= 0) {
            items[index] = items[index].copy(alias = alias.trim().ifEmpty { null })
            putAsync(items)
        }
        return items
    }

    suspend fun isFavoriteAsync(fullPath: String): Boolean {
        return getValueAsync().any { it.fullPath == fullPath }
    }
}

object ScanHistoryPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("scan_history")

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
    override val key = stringPreferenceKey("audio_playing")

    suspend fun getValueAsync(): String {
        val str = getAsync()
        if (str.isEmpty() || str.startsWith("{")) return ""
        return str
    }
}

/** One-shot flag: legacy audio_playlist JSON imported into the Room queue tables. */
object AudioQueueMigratedPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("audio_queue_migrated")
}

object AudioPlaylistPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("audio_playlist")

    suspend fun getValueAsync(): List<DPlaylistAudio> {
        val str = getAsync()
        if (str.isEmpty()) return listOf()
        return try {
            jsonDecode(str)
        } catch (_: Exception) {
            listOf()
        }
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

object ChatInputTextPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("chat_input_text")
}

object NearbyDiscoverablePreference : BasePreference<Boolean>() {
    override val default = true
    override val key = booleanPreferenceKey("nearby_discoverable")
}

object MdnsHostnamePreference : BasePreference<String>() {
    override val default = "plainapp.local"
    override val key = stringPreferenceKey("mdns_hostname")

    suspend fun ensureValueAsync(preferences: PreferenceSnapshot) {
        val stored = preferences[key]
        if (stored.isNullOrEmpty()) {
            val allowedChars = ('a'..'z').filter { it !in listOf('i', 'l', 'o', 'v') }
            val randomString = (1..2).map { allowedChars.random() }.joinToString("")
            val hostname = "$randomString.local"
            TempData.mdnsHostname = hostname
            putAsync(hostname)
        } else {
            TempData.mdnsHostname = stored
        }
    }

}

object AiImageSearchEnabledPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("ai_image_search_enabled")
}

object DocTabsModePreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("doc_tabs_mode")
}

object FidUriExtMigratedPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("fid_uri_ext_migrated")
}

object AppFileRealPathMigratedPreference : BasePreference<Boolean>() {
    override val default = false
    override val key = booleanPreferenceKey("app_file_real_path_migrated")
}

object NotificationFilterPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("notification_filter")

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
    override val key = stringPreferenceKey("pomodoro_settings")

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
    override val key = stringPreferenceKey("signature_key_pair")

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun ensureKeyPairAsync() {
        val keyPairJson = getAsync()
        if (keyPairJson.isEmpty()) {
            val (privateKey, publicKey) = generateEd25519KeyPair()
            val signatureKeyPair = DSignatureKeyPair(
                privateKey = Base64.encode(privateKey),
                publicKey = Base64.encode(publicKey),
            )
            putAsync(jsonEncode(signatureKeyPair))
        }
    }

    suspend fun getKeyPairAsync(): DSignatureKeyPair {
        return jsonDecode<DSignatureKeyPair>(getAsync())
    }

    suspend fun getPublicKeyBytesAsync(): ByteArray {
        val keyPair = getKeyPairAsync()
        return Base64Lenient.decode(keyPair.publicKey)
    }
}

object VideoPlaylistPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("video_playlist")

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
    override val key = stringSetPreferenceKey("dlna_allowed_senders")

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
    override val key = stringSetPreferenceKey("dlna_denied_senders")

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

object HomeFeaturesPreference : BasePreference<String>() {
    private const val SEPARATOR = "|"
    override val default = listOf(
        AppFeatureType.IMAGES, AppFeatureType.VIDEOS, AppFeatureType.AUDIO,
        AppFeatureType.DOCS, AppFeatureType.FILES, AppFeatureType.NOTES, AppFeatureType.FEEDS,
    ).joinToString(SEPARATOR) { it.name }
    override val key = stringPreferenceKey("home_features_v2")

    fun parseList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR).filter { it.isNotBlank() }

    fun formatList(list: List<String>): String = list.joinToString(SEPARATOR)
}

object QuickNoteDraftPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("quick_note_draft")
}

object LauncherShortcutsPreference : BasePreference<String>() {
    private const val SEPARATOR = "|"
    override val default = com.ismartcoding.plain.ui.nav.LauncherShortcutTools.DEFAULT.joinToString(SEPARATOR) { it.name }
    override val key = stringPreferenceKey("launcher_shortcuts_v1")

    fun parseList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR).filter { it.isNotBlank() }

    fun formatList(list: List<com.ismartcoding.plain.enums.AppFeatureType>): String =
        list.joinToString(SEPARATOR) { it.name }

    /** Selected shortcut tools, matching known candidates by name. */
    fun selected(value: String): List<com.ismartcoding.plain.enums.AppFeatureType> =
        parseList(value).mapNotNull { name ->
            com.ismartcoding.plain.ui.nav.LauncherShortcutTools.ALL.firstOrNull { it.name == name }
        }
}

object HomeSectionCollapsedPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferenceKey("home_section_collapsed")

    fun get(preferences: PreferenceSnapshot, feature: AppFeatureType): Boolean {
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
            preferencesJson.decodeFromString<Map<String, Boolean>>(value).mapNotNull { (key, collapsed) ->
                runCatching { AppFeatureType.valueOf(key) }.getOrNull()?.let { it to collapsed }
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun formatMap(value: Map<AppFeatureType, Boolean>): String {
        return preferencesJson.encodeToString(value.mapKeys { it.key.name })
    }
}
