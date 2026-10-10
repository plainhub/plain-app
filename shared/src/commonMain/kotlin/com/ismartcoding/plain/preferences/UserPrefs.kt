package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.Locale
import com.ismartcoding.plain.platform.setSystemLocale

object UserPrefs {
    private val client get() = PreferencesClient.local
    val httpPort = RemoteSetting<Int>(PreferencesClient.local, { it.httpPort }, { UserSettingsPatch(httpPort = it) })
    val httpsPort = RemoteSetting<Int>(PreferencesClient.local, { it.httpsPort }, { UserSettingsPatch(httpsPort = it) })
    val darkTheme = RemoteSetting<Int>(PreferencesClient.local, { it.darkTheme }, { UserSettingsPatch(darkTheme = it) })
    val amoledDarkTheme = RemoteSetting<Boolean>(PreferencesClient.local, { it.amoledDarkTheme }, { UserSettingsPatch(amoledDarkTheme = it) })
    val pdfFollowDarkTheme = RemoteSetting<Boolean>(PreferencesClient.local, { it.pdfFollowDarkTheme }, { UserSettingsPatch(pdfFollowDarkTheme = it) })
    val keepAwake = RemoteSetting<Boolean>(PreferencesClient.local, { it.keepAwake }, { UserSettingsPatch(keepAwake = it) })
    val locale = RemoteSetting<String>(PreferencesClient.local, { it.locale }, { UserSettingsPatch(locale = it) })
    val service = RemoteSetting<Boolean>(PreferencesClient.local, { it.service }, { UserSettingsPatch(service = it) })
    val desktopAccess = RemoteSetting<Boolean>(PreferencesClient.local, { it.desktopAccess }, { UserSettingsPatch(desktopAccess = it) })
    val dlna = RemoteSetting<Boolean>(PreferencesClient.local, { it.dlna }, { UserSettingsPatch(dlna = it) })
    val developerMode = RemoteSetting<Boolean>(PreferencesClient.local, { it.developerMode }, { UserSettingsPatch(developerMode = it) })
    val allowAnyHost = RemoteSetting<Boolean>(PreferencesClient.local, { it.allowAnyHost }, { UserSettingsPatch(allowAnyHost = it) })
    val deviceName = RemoteSetting<String>(PreferencesClient.local, { it.deviceName }, { UserSettingsPatch(deviceName = it) })
    val https = RemoteSetting<Boolean>(PreferencesClient.local, { it.https }, { UserSettingsPatch(https = it) })
    val webAddressBarExpanded = RemoteSetting<Boolean>(PreferencesClient.local, { it.webAddressBarExpanded }, { UserSettingsPatch(webAddressBarExpanded = it) })
    val screenMirrorQuality = RemoteSetting<DScreenMirrorQuality>(PreferencesClient.local, { it.screenMirrorQuality }, { UserSettingsPatch(screenMirrorQuality = it) })
    val audioPlayMode = RemoteSetting<MediaPlayMode>(PreferencesClient.local, { MediaPlayMode.entries[it.audioPlayMode] }, { UserSettingsPatch(audioPlayMode = it.ordinal) })
    val audioPlaybackSpeed = RemoteSetting<Float>(PreferencesClient.local, { it.audioPlaybackSpeed }, { UserSettingsPatch(audioPlaybackSpeed = it) })
    val imageGridCellsPerRow = RemoteSetting<Int>(PreferencesClient.local, { it.imageGridCellsPerRow }, { UserSettingsPatch(imageGridCellsPerRow = it) })
    val videoGridCellsPerRow = RemoteSetting<Int>(PreferencesClient.local, { it.videoGridCellsPerRow }, { UserSettingsPatch(videoGridCellsPerRow = it) })
    val showHiddenFiles = RemoteSetting<Boolean>(PreferencesClient.local, { it.showHiddenFiles }, { UserSettingsPatch(showHiddenFiles = it) })
    val feedAutoRefresh = RemoteSetting<Boolean>(PreferencesClient.local, { it.feedAutoRefresh }, { UserSettingsPatch(feedAutoRefresh = it) })
    val feedAutoRefreshInterval = RemoteSetting<Int>(PreferencesClient.local, { it.feedAutoRefreshIntervalSec }, { UserSettingsPatch(feedAutoRefreshIntervalSec = it) })
    val feedAutoRefreshOnlyWifi = RemoteSetting<Boolean>(PreferencesClient.local, { it.feedAutoRefreshOnlyWifi }, { UserSettingsPatch(feedAutoRefreshOnlyWifi = it) })
    val feedFontScale = RemoteSetting<Int>(PreferencesClient.local, { it.feedFontScale }, { UserSettingsPatch(feedFontScale = it) })
    val editorWrapContent = RemoteSetting<Boolean>(PreferencesClient.local, { it.editorWrapContent }, { UserSettingsPatch(editorWrapContent = it) })
    val editorFontSize = RemoteSetting<Int>(PreferencesClient.local, { it.editorFontSize }, { UserSettingsPatch(editorFontSize = it) })
    val editorStatusBar = RemoteSetting<Boolean>(PreferencesClient.local, { it.editorStatusBar }, { UserSettingsPatch(editorStatusBar = it) })
    val lastFilePath = RemoteSetting<FilePathData>(PreferencesClient.local, { it.lastFilePath }, { UserSettingsPatch(lastFilePath = it) })
    val scanHistory = RemoteSetting<List<String>>(PreferencesClient.local, { it.scanHistory }, { UserSettingsPatch(scanHistory = it) })
    val audioPlaylist = RemoteSetting<List<com.ismartcoding.plain.audio.DPlaylistAudio>>(PreferencesClient.local, { it.audioPlaylist }, { UserSettingsPatch(audioPlaylist = it) })
    val chatInputText = RemoteSetting<String>(PreferencesClient.local, { it.chatInputText }, { UserSettingsPatch(chatInputText = it) })
    val nearbyDiscoverable = RemoteSetting<Boolean>(PreferencesClient.local, { it.nearbyDiscoverable }, { UserSettingsPatch(nearbyDiscoverable = it) })
    val aiImageSearchEnabled = RemoteSetting<Boolean>(PreferencesClient.local, { it.aiImageSearchEnabled }, { UserSettingsPatch(aiImageSearchEnabled = it) })
    val notificationFilter = RemoteSetting<NotificationFilterData>(PreferencesClient.local, { it.notificationFilter }, { UserSettingsPatch(notificationFilter = it) })
    val pomodoroSettings = RemoteSetting<DPomodoroSettings>(PreferencesClient.local, { it.pomodoroSettings }, { UserSettingsPatch(pomodoroSettings = it) })
    val videoPlaylist = RemoteSetting<List<DVideo>>(PreferencesClient.local, { it.videoPlaylist }, { UserSettingsPatch(videoPlaylist = it) })
    val homeFeatures = RemoteSetting<String>(PreferencesClient.local, { it.homeFeatures }, { UserSettingsPatch(homeFeatures = it) })
    val quickNoteDraft = RemoteSetting<String>(PreferencesClient.local, { it.quickNoteDraft }, { UserSettingsPatch(quickNoteDraft = it) })
    val launcherShortcuts = RemoteSetting<String>(PreferencesClient.local, { it.launcherShortcuts }, { UserSettingsPatch(launcherShortcuts = it) })
    val recentSearches = RemoteSetting<List<String>>(PreferencesClient.local, { it.recentSearches }, { UserSettingsPatch(recentSearches = it) })
    val recentSaveDirs = RemoteSetting<List<String>>(PreferencesClient.local, { it.recentSaveDirs }, { UserSettingsPatch(recentSaveDirs = it) })
    val audioSortBy = RemoteSetting<Int>(PreferencesClient.local, { it.audioSortBy }, { UserSettingsPatch(audioSortBy = it) })
    val videoSortBy = RemoteSetting<Int>(PreferencesClient.local, { it.videoSortBy }, { UserSettingsPatch(videoSortBy = it) })
    val imageSortBy = RemoteSetting<Int>(PreferencesClient.local, { it.imageSortBy }, { UserSettingsPatch(imageSortBy = it) })
    val docSortBy = RemoteSetting<Int>(PreferencesClient.local, { it.docSortBy }, { UserSettingsPatch(docSortBy = it) })
    val fileSortBy = RemoteSetting<Int>(PreferencesClient.local, { it.fileSortBy }, { UserSettingsPatch(fileSortBy = it) })
    val packageSortBy = RemoteSetting<Int>(PreferencesClient.local, { it.packageSortBy }, { UserSettingsPatch(packageSortBy = it) })

    suspend fun setDarkThemeValue(value: Int) { darkTheme.set(value) }
    fun localeValue(): Locale? = parseLocale(locale.value)
    fun parseLocale(value: String): Locale? = value.takeIf { it.isNotEmpty() }?.split('-')?.let { Locale(it[0], it.getOrElse(1) { "" }) }
    suspend fun setLocale(locale: Locale?) {
        this.locale.set(locale?.let { it.language + if (it.country.isEmpty()) "" else "-${it.country}" }.orEmpty())
    }
    fun screenMirrorQualityValue() = screenMirrorQuality.value
    suspend fun setScreenMirrorQuality(value: DScreenMirrorQuality) { screenMirrorQuality.set(value) }
    fun lastFilePathValue() = lastFilePath.value
    suspend fun setLastFilePath(value: FilePathData) { lastFilePath.set(value) }
    fun scanHistoryValue() = scanHistory.value
    suspend fun setScanHistory(value: List<String>) { scanHistory.set(value) }
    fun audioPlaylistValue() = audioPlaylist.value
    suspend fun setAudioPlaylist(value: List<com.ismartcoding.plain.audio.DPlaylistAudio>) { audioPlaylist.set(value) }
    fun notificationFilterValue() = notificationFilter.value
    suspend fun setNotificationFilter(value: NotificationFilterData) { notificationFilter.set(value) }
    suspend fun toggleNotificationApp(packageName: String) = client.toggleNotificationApp(packageName)
    suspend fun clearNotificationApps() = client.clearNotificationFilterApps()
    suspend fun setNotificationMode(mode: String) = client.setNotificationFilterMode(mode)
    fun pomodoroSettingsValue() = pomodoroSettings.value
    suspend fun setPomodoroSettings(value: DPomodoroSettings) { pomodoroSettings.set(value) }
    fun videoPlaylistValue() = videoPlaylist.value
    suspend fun setVideoPlaylist(value: List<DVideo>) { videoPlaylist.set(value) }
    suspend fun deleteVideos(paths: Set<String>) = run {
        client.deletePlaylistVideos(paths)
    }
    fun parseFeatures(value: String): List<String> = value.split('|').filter { it.isNotBlank() }
    fun formatFeatures(value: List<String>): String = value.joinToString("|")
    suspend fun setHomeFeatures(value: String) { homeFeatures.set(value) }
    fun selectedLauncherShortcuts(value: String): List<AppFeatureType> = parseFeatures(value).mapNotNull { name -> AppFeatureType.entries.find { it.name == name } }
    fun formatLauncherShortcuts(value: List<AppFeatureType>): String = value.joinToString("|") { it.name }
    suspend fun setLauncherShortcuts(value: String) { launcherShortcuts.set(value) }
    fun recentSearchesValue() = recentSearches.value
    suspend fun recordRecentSearch(term: String): List<String> { client.recordRecentSearch(term); return recentSearches.value }
    suspend fun removeRecentSearch(term: String): List<String> { client.removeRecentSearch(term); return recentSearches.value }
    suspend fun clearRecentSearches() = client.clearRecentSearches()
    fun recentSaveDirsValue() = recentSaveDirs.value
    suspend fun recordRecentSaveDir(dir: String): List<String> { client.recordRecentSaveDirectory(dir); return recentSaveDirs.value }
    fun audioSortByValue() = FileSortBy.entries[audioSortBy.value]
    suspend fun setAudioSortBy(value: FileSortBy) { audioSortBy.set(value.ordinal) }
    fun videoSortByValue() = FileSortBy.entries[videoSortBy.value]
    suspend fun setVideoSortBy(value: FileSortBy) { videoSortBy.set(value.ordinal) }
    fun imageSortByValue() = FileSortBy.entries[imageSortBy.value]
    suspend fun setImageSortBy(value: FileSortBy) { imageSortBy.set(value.ordinal) }
    fun docSortByValue() = FileSortBy.entries[docSortBy.value]
    suspend fun setDocSortBy(value: FileSortBy) { docSortBy.set(value.ordinal) }
    fun fileSortByValue() = FileSortBy.entries[fileSortBy.value]
    suspend fun setFileSortBy(value: FileSortBy) { fileSortBy.set(value.ordinal) }
    fun packageSortByValue() = FileSortBy.entries[packageSortBy.value]
    suspend fun setPackageSortBy(value: FileSortBy) { packageSortBy.set(value.ordinal) }
}
