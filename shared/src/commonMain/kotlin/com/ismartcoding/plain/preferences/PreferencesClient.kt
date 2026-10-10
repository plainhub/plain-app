package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.api.ContentApiSession
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonElement

class PreferencesClient(private val session: ContentApiSession? = null) {
    private var systemProjection: MutableStateFlow<SystemState>? = null
    private var userProjection: MutableStateFlow<UserSettings>? = null
    private val mutex = Mutex()
    val system: StateFlow<SystemState> get() = checkNotNull(systemProjection).asStateFlow()
    val user: StateFlow<UserSettings> get() = checkNotNull(userProjection).asStateFlow()

    suspend fun refresh() = mutex.withLock { refreshLocked() }
    private suspend fun refreshLocked() {
        val result = RustContentApi.graphql("query { $SELECTION }", session = session)
        apply(result)
    }
    private suspend fun apply(result: kotlinx.serialization.json.JsonObject) {
        val system = JsonHelper.jsonDecodeFromElement<SystemState>(result.getValue("systemSettings"))
        val user = JsonHelper.jsonDecodeFromElement<UserSettings>(result.getValue("userSettings"))
        systemProjection?.let { it.value = system } ?: run { systemProjection = MutableStateFlow(system) }
        val previous = userProjection?.value
        userProjection?.let { it.value = user } ?: run { userProjection = MutableStateFlow(user) }
        if (session == null && previous != null) {
            if (previous.deviceName != user.deviceName) com.ismartcoding.plain.TempData.deviceName.value = user.deviceName
            if (previous.screenMirrorQuality != user.screenMirrorQuality) com.ismartcoding.plain.platform.onScreenMirrorQualityChanged(user.screenMirrorQuality.mode)
            if (previous.dlna != user.dlna) {
                if (user.dlna) com.ismartcoding.plain.features.dlna.DlnaRendererState.start()
                else com.ismartcoding.plain.features.dlna.DlnaRendererState.stop()
            }
            if (previous.feedAutoRefresh != user.feedAutoRefresh || previous.feedAutoRefreshIntervalSec != user.feedAutoRefreshIntervalSec || previous.feedAutoRefreshOnlyWifi != user.feedAutoRefreshOnlyWifi) {
                if (user.feedAutoRefresh) com.ismartcoding.plain.platform.feedWorkerStartRepeat()
                else com.ismartcoding.plain.platform.feedWorkerCancelRepeat()
            }
            if (previous.locale != user.locale) com.ismartcoding.plain.platform.setSystemLocale(UserPrefs.localeValue())
            if (previous.service != user.service) {
                if (user.service) com.ismartcoding.plain.platform.HttpServerManager.requestStart(fromUi = false)
                else com.ismartcoding.plain.platform.stopHttpServiceAsync()
            }
        }
    }

    suspend fun updateSystem(command: SystemCommand) = mutex.withLock {
        val result = RustContentApi.graphql("mutation(\$input: SystemSettingsInput!) { updateSystemSettings(input: \$input) { $SELECTION } }",
            mapOf("input" to JsonHelper.jsonEncodeToElement<SystemCommand>(command)), session)
        apply(result.getValue("updateSystemSettings").jsonObject)
    }
    suspend fun patchUser(patch: UserSettingsPatch) = mutex.withLock {
        val result = RustContentApi.graphql("mutation(\$input: UserSettingsInput!) { updateUserSettings(input: \$input) { $SELECTION } }",
            mapOf("input" to JsonHelper.jsonEncodeToElement(patch)), session)
        apply(result.getValue("updateUserSettings").jsonObject)
    }
    private suspend fun action(field: String, argument: String? = null, type: String = "String!", value: JsonElement? = null) = mutex.withLock {
        val declaration = if (argument == null) "" else "(\$value: $type)"
        val args = if (argument == null) "" else "($argument: \$value)"
        val variables = if (argument == null) emptyMap() else mapOf("value" to checkNotNull(value))
        RustContentApi.graphql("mutation$declaration { $field$args { affectedCount } }", variables, session)
        refreshLocked()
    }
    suspend fun recordRecentSearch(query: String) = action("recordRecentSearch", "query", value = JsonHelper.jsonEncodeToElement(query))
    suspend fun removeRecentSearch(query: String) = action("removeRecentSearch", "query", value = JsonHelper.jsonEncodeToElement(query))
    suspend fun clearRecentSearches() = action("clearRecentSearches")
    suspend fun recordRecentSaveDirectory(path: String) = action("recordRecentSaveDirectory", "path", value = JsonHelper.jsonEncodeToElement(path))
    suspend fun toggleNotificationApp(packageName: String) = action("toggleNotificationApp", "packageName", value = JsonHelper.jsonEncodeToElement(packageName))
    suspend fun setNotificationFilterMode(mode: String) = action("setNotificationFilterMode", "mode", value = JsonHelper.jsonEncodeToElement(mode))
    suspend fun clearNotificationFilterApps() = action("clearNotificationFilterApps")
    suspend fun deletePlaylistVideos(paths: Set<String>) = action("deletePlaylistVideos", "paths", "[String!]!", JsonHelper.jsonEncodeToElement(paths))
    suspend fun verifyAdbToken(token: String): Boolean = JsonHelper.jsonDecodeFromElement(
        RustContentApi.graphql("query(\$token: String!) { verifyAdbToken(token: \$token) }", mapOf("token" to JsonHelper.jsonEncodeToElement(token)), session).getValue("verifyAdbToken"))

    companion object {
        private const val SELECTION = "systemSettings { password passwordType authTwoFactor rotateUrlTokenOnRestart adbToken updateInfo urlToken apiPermissions onboardingCompleted clientId mdnsHostname signaturePublicKey } userSettings { httpPort httpsPort darkTheme amoledDarkTheme pdfFollowDarkTheme keepAwake locale service desktopAccess dlna developerMode allowAnyHost deviceName https webAddressBarExpanded screenMirrorQuality audioPlayMode audioPlaybackSpeed imageGridCellsPerRow videoGridCellsPerRow showHiddenFiles feedAutoRefresh feedAutoRefreshIntervalSec feedAutoRefreshOnlyWifi feedFontScale editorWrapContent editorFontSize editorStatusBar lastFilePath scanHistory audioPlaylist chatInputText nearbyDiscoverable aiImageSearchEnabled notificationFilter pomodoroSettings videoPlaylist homeFeatures quickNoteDraft launcherShortcuts recentSearches recentSaveDirs audioSortBy videoSortBy imageSortBy docSortBy fileSortBy packageSortBy }"
        val local = PreferencesClient()
    }
}
