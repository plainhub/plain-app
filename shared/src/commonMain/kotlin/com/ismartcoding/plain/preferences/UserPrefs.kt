package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.data.FilePathData
import com.ismartcoding.plain.data.NotificationFilterData
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.JsonHelper.jsonDecode
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.platform.Locale
import com.ismartcoding.plain.platform.setSystemLocale
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object UserPrefs {
    private inline fun <reified T> flow(key: String, default: T) = Prefs.userFlow(key, default)

    val httpPort = flow("http_port", 8080)
    val httpsPort = flow("https_port", 8443)
    val darkTheme = flow("dark_theme", DarkTheme.UseDeviceTheme.value)
    val amoledDarkTheme = flow("amoled_dark_theme", false)
    val pdfFollowDarkTheme = flow("pdf_follow_dark_theme", false)
    val keepAwake = flow("keep_awake", true)
    val locale = flow("locale", "")
    val service = flow("service", false)
    val desktopAccess = flow("desktop_access", true)
    val dlna = flow("dlna", false)
    val developerMode = flow("developer_mode", false)
    val allowAnyHost = flow("allow_any_host", false)
    val deviceName = flow("device_name", "")
    val https = flow("https", false)
    val webAddressBarExpanded = flow("web_address_bar_expanded", false)
    val screenMirrorQuality = flow("screen_mirror_quality", "")
    val audioPlayMode = Prefs.userFlow("audio_play_mode", MediaPlayMode.REPEAT, MediaPlayModeSerializer)
    val audioPlaybackSpeed = flow("audio_playback_speed", 1f)
    val imageGridCellsPerRow = flow("image_grid_cells_per_row", 3)
    val videoGridCellsPerRow = flow("video_grid_cells_per_row", 3)
    val showHiddenFiles = flow("show_hidden_files", false)
    val feedAutoRefresh = flow("feed_auto_refresh", true)
    val feedAutoRefreshInterval = flow("feed_auto_refresh_interval", 7200)
    val feedAutoRefreshOnlyWifi = flow("feed_auto_refresh_only_wifi", false)
    val feedFontScale = flow("feed_font_scale", 1)
    val editorWrapContent = flow("editor_wrap_content", true)
    val editorFontSize = flow("editor_font_size", 14)
    val editorStatusBar = flow("editor_status_bar", true)
    val lastFilePath = flow("last_file_path", "")
    val favoriteFolders = flow("favorite_folders", "")
    val scanHistory = flow("scan_history", "")
    val audioPlaylist = flow("audio_playlist", "")
    val chatInputText = flow("chat_input_text", "")
    val nearbyDiscoverable = flow("nearby_discoverable", true)
    val aiImageSearchEnabled = flow("ai_image_search_enabled", false)
    val notificationFilter = flow("notification_filter", "")
    val pomodoroSettings = flow("pomodoro_settings", "")
    val videoPlaylist = flow("video_playlist", "")
    val dlnaAllowedSenders = flow("dlna_allowed_senders", setOf<String>())
    val dlnaDeniedSenders = flow("dlna_denied_senders", setOf<String>())
    val homeFeatures = flow("home_features_v2", listOf(AppFeatureType.IMAGES, AppFeatureType.VIDEOS, AppFeatureType.AUDIO, AppFeatureType.DOCS, AppFeatureType.FILES, AppFeatureType.NOTES, AppFeatureType.FEEDS).joinToString("|") { it.name })
    val quickNoteDraft = flow("quick_note_draft", "")
    val launcherShortcuts = flow("launcher_shortcuts_v1", com.ismartcoding.plain.ui.nav.LauncherShortcutTools.DEFAULT.joinToString("|") { it.name })
    val recentSearches = flow("recent_searches", "")
    val recentSaveDirs = flow("recent_save_dirs", "")
    val audioSortBy = flow("audio_sort_by", FileSortBy.DATE_DESC.ordinal)
    val videoSortBy = flow("video_sort_by", FileSortBy.TAKEN_AT_DESC.ordinal)
    val imageSortBy = flow("image_sort_by", FileSortBy.TAKEN_AT_DESC.ordinal)
    val docSortBy = flow("doc_sort_by", FileSortBy.DATE_DESC.ordinal)
    val fileSortBy = flow("file_sort_by", FileSortBy.NAME_ASC.ordinal)
    val packageSortBy = flow("pkg_sort_by", FileSortBy.NAME_ASC.ordinal)

    val snapshot: Map<String, JsonElement> get() = Prefs.userSnapshot

    internal fun initialize() = Unit

    fun set(key: String, value: JsonElement) = Prefs.setUserPref(key, value)
    fun remove(key: String) = Prefs.removeUserPref(key)

    fun setDarkThemeValue(value: Int) { darkTheme.value = value }
    fun localeValue(): Locale? = Prefs.parseLocale(locale.value)
    fun setLocale(locale: Locale?) {
        this.locale.value = if (locale == null) "" else locale.language + if (locale.country.isEmpty()) "" else "-${locale.country}"
        setSystemLocale(locale)
    }

    fun screenMirrorQualityValue(): DScreenMirrorQuality = Prefs.decodeOrDefault(screenMirrorQuality.value) { DScreenMirrorQuality() }
    fun setScreenMirrorQuality(value: DScreenMirrorQuality) { screenMirrorQuality.value = Prefs.json.encodeToString(value) }

    fun lastFilePathValue(): FilePathData = Prefs.decodeOrDefault(lastFilePath.value) { FilePathData("", "", "") }
    fun setLastFilePath(value: FilePathData) { lastFilePath.value = Prefs.json.encodeToString(value) }

    fun favoriteFoldersValue(): List<DFavoriteFolder> = Prefs.decodeOrDefault(favoriteFolders.value) { emptyList() }
    fun setFavoriteFolders(value: List<DFavoriteFolder>) { favoriteFolders.value = Prefs.json.encodeToString(value) }
    fun addFavoriteFolder(folder: DFavoriteFolder): List<DFavoriteFolder> = favoriteFoldersValue().toMutableList().also {
        it.removeAll { item -> item.fullPath == folder.fullPath }
        it.add(folder)
        setFavoriteFolders(it)
    }
    fun removeFavoriteFolder(fullPath: String): List<DFavoriteFolder> = favoriteFoldersValue().toMutableList().also {
        it.removeAll { item -> item.fullPath == fullPath }
        setFavoriteFolders(it)
    }
    fun renameFavoriteFolder(fullPath: String, alias: String): List<DFavoriteFolder> = favoriteFoldersValue().toMutableList().also {
        val index = it.indexOfFirst { item -> item.fullPath == fullPath }
        if (index >= 0) {
            it[index] = it[index].copy(alias = alias.trim().ifEmpty { null })
            setFavoriteFolders(it)
        }
    }
    fun isFavoriteFolder(fullPath: String) = favoriteFoldersValue().any { it.fullPath == fullPath }

    fun scanHistoryValue(): List<String> = Prefs.decodeOrDefault(scanHistory.value) { emptyList() }
    fun setScanHistory(value: List<String>) { scanHistory.value = Prefs.json.encodeToString(value) }

    fun audioPlaylistValue(): List<com.ismartcoding.plain.audio.DPlaylistAudio> = runCatching { jsonDecode<List<com.ismartcoding.plain.audio.DPlaylistAudio>>(audioPlaylist.value) }.getOrDefault(emptyList())
    fun setAudioPlaylist(value: List<com.ismartcoding.plain.audio.DPlaylistAudio>) { audioPlaylist.value = jsonEncode(value) }

    fun notificationFilterValue(): NotificationFilterData = Prefs.decodeOrDefault(notificationFilter.value) { NotificationFilterData() }
    fun setNotificationFilter(value: NotificationFilterData) { notificationFilter.value = Prefs.json.encodeToString(value) }
    fun toggleNotificationApp(packageName: String) {
        val data = notificationFilterValue()
        val apps = data.apps.toMutableSet()
        if (!apps.add(packageName)) apps.remove(packageName)
        setNotificationFilter(data.copy(apps = apps))
    }
    fun setNotificationMode(mode: String) { setNotificationFilter(notificationFilterValue().copy(mode = mode)) }
    fun isNotificationAllowed(packageName: String): Boolean {
        val data = notificationFilterValue()
        return when (data.mode) {
            "allowlist" -> packageName in data.apps
            "blacklist" -> packageName !in data.apps
            else -> true
        }
    }

    fun pomodoroSettingsValue(): DPomodoroSettings = Prefs.decodeOrDefault(pomodoroSettings.value) { DPomodoroSettings() }
    fun setPomodoroSettings(value: DPomodoroSettings) { pomodoroSettings.value = Prefs.json.encodeToString(value) }

    fun videoPlaylistValue(): List<DVideo> = Prefs.decodeOrDefault(videoPlaylist.value) { emptyList() }
    fun setVideoPlaylist(value: List<DVideo>) { videoPlaylist.value = Prefs.json.encodeToString(value) }
    fun deleteVideos(paths: Set<String>) { setVideoPlaylist(videoPlaylistValue().filterNot { it.path in paths }) }

    fun addAllowedDlnaSender(ip: String, name: String) { dlnaAllowedSenders.value = Prefs.senderEntriesWith(dlnaAllowedSenders.value, ip, name) }
    fun removeAllowedDlnaSender(ip: String) { dlnaAllowedSenders.value = Prefs.senderEntriesWithout(dlnaAllowedSenders.value, ip) }
    fun addDeniedDlnaSender(ip: String, name: String) { dlnaDeniedSenders.value = Prefs.senderEntriesWith(dlnaDeniedSenders.value, ip, name) }
    fun removeDeniedDlnaSender(ip: String) { dlnaDeniedSenders.value = Prefs.senderEntriesWithout(dlnaDeniedSenders.value, ip) }
    fun containsDlnaSender(entries: Set<String>, ip: String) = entries.any { Prefs.decodeSenderEntry(it).first == ip }

    fun parseFeatures(value: String): List<String> = parseNames(value)
    fun formatFeatures(value: List<String>): String = value.joinToString("|")
    fun setHomeFeatures(value: String) { homeFeatures.value = value }
    fun selectedLauncherShortcuts(value: String): List<AppFeatureType> = parseNames(value).mapNotNull { name ->
        com.ismartcoding.plain.ui.nav.LauncherShortcutTools.ALL.firstOrNull { it.name == name }
    }
    fun formatLauncherShortcuts(value: List<AppFeatureType>): String = value.joinToString("|") { it.name }
    fun setLauncherShortcuts(value: String) { launcherShortcuts.value = value }

    fun recentSearchesValue(): List<String> = Prefs.decodeOrDefault(recentSearches.value) { emptyList() }
    fun recordRecentSearch(term: String): List<String> = recentSearchesValue().toMutableList().also {
        it.removeAll { item -> item.equals(term, ignoreCase = true) }
        it.add(0, term)
        while (it.size > 10) it.removeAt(it.lastIndex)
        recentSearches.value = Prefs.json.encodeToString(it)
    }
    fun removeRecentSearch(term: String): List<String> = recentSearchesValue().toMutableList().also {
        it.removeAll { item -> item.equals(term, ignoreCase = true) }
        recentSearches.value = Prefs.json.encodeToString(it)
    }
    fun clearRecentSearches() { recentSearches.value = Prefs.json.encodeToString(emptyList<String>()) }

    fun recentSaveDirsValue(): List<String> = Prefs.decodeOrDefault(recentSaveDirs.value) { emptyList() }
    fun recordRecentSaveDir(dir: String): List<String> = recentSaveDirsValue().toMutableList().also {
        it.removeAll { item -> item == dir }
        it.add(0, dir)
        while (it.size > 5) it.removeAt(it.lastIndex)
        recentSaveDirs.value = Prefs.json.encodeToString(it)
    }

    fun audioSortByValue() = parseSort(audioSortBy.value, FileSortBy.DATE_DESC)
    fun setAudioSortBy(value: FileSortBy) { audioSortBy.value = value.ordinal }
    fun videoSortByValue() = parseSort(videoSortBy.value, FileSortBy.TAKEN_AT_DESC)
    fun setVideoSortBy(value: FileSortBy) { videoSortBy.value = value.ordinal }
    fun imageSortByValue() = parseSort(imageSortBy.value, FileSortBy.TAKEN_AT_DESC)
    fun setImageSortBy(value: FileSortBy) { imageSortBy.value = value.ordinal }
    fun docSortByValue() = parseSort(docSortBy.value, FileSortBy.DATE_DESC)
    fun setDocSortBy(value: FileSortBy) { docSortBy.value = value.ordinal }
    fun fileSortByValue() = parseSort(fileSortBy.value, FileSortBy.NAME_ASC)
    fun setFileSortBy(value: FileSortBy) { fileSortBy.value = value.ordinal }
    fun packageSortByValue() = parseSort(packageSortBy.value, FileSortBy.NAME_ASC)
    fun setPackageSortBy(value: FileSortBy) { packageSortBy.value = value.ordinal }

    private fun parseSort(value: Int, default: FileSortBy): FileSortBy = FileSortBy.entries.find { it.ordinal == value } ?: default
    private fun parseNames(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split("|").filter { it.isNotBlank() }
}

internal object MediaPlayModeSerializer : KSerializer<MediaPlayMode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MediaPlayMode", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: MediaPlayMode) = encoder.encodeInt(value.ordinal)

    override fun deserialize(decoder: Decoder): MediaPlayMode =
        MediaPlayMode.entries.getOrElse(decoder.decodeInt()) { MediaPlayMode.REPEAT }
}