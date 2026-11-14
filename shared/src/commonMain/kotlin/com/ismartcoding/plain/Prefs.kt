package com.ismartcoding.plain

import com.ismartcoding.plain.features.audio.MediaPlayMode
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.models.AudioSortBy
import com.ismartcoding.plain.models.VideoSortBy
import com.ismartcoding.plain.models.ImageSortBy
import com.ismartcoding.plain.models.DocSortBy
import com.ismartcoding.plain.features.notifications.NotificationFilterData
import com.ismartcoding.plain.features.notifications.NotificationFilterMode
import com.ismartcoding.plain.features.screenmirror.ScreenMirrorMode
import com.ismartcoding.plain.features.settings.PasswordType
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.PomodoroSettings
import com.ismartcoding.plain.models.ScreenMirrorQuality
import com.ismartcoding.plain.models.UpdateInfo
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.Locale
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.native.nativeValue
import com.ismartcoding.plain.ui.theme.DarkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

object Prefs {
    val passwordType = MutableStateFlow(PasswordType.NONE)
    val passwordConfigured = MutableStateFlow<Boolean?>(null)
    val loginRequireApproval = MutableStateFlow(false)
    val apiAccessFeatures = MutableStateFlow(emptySet<com.ismartcoding.plain.features.web.AccessFeature>())
    val updateInfo = MutableStateFlow(
        UpdateInfo(
            newVersion = "",
            checkUpdateTime = 0,
            publishDate = "",
            log = "",
            downloadUrl = "",
            size = 0,
            downloadedApkPath = "",
        )
    )
    val appUpdateAvailable = MutableStateFlow(false)
    val autoCheckUpdate = MutableStateFlow(false)
    val adbToken = MutableStateFlow("")
    val httpPort = MutableStateFlow(0)
    val httpsPort = MutableStateFlow(0)
    val darkTheme = MutableStateFlow(DarkTheme.USE_DEVICE_THEME)
    val amoledDarkTheme = MutableStateFlow(false)
    val pdfFollowDarkTheme = MutableStateFlow(false)
    val locale = MutableStateFlow("")
    val backgroundServiceEnabled = MutableStateFlow(false)
    val remoteAccess = MutableStateFlow(false)
    val dlna = MutableStateFlow(false)
    val developerMode = MutableStateFlow(false)
    val allowAnyHost = MutableStateFlow(false)
    val https = MutableStateFlow(false)
    val webAddressBarExpanded = MutableStateFlow(false)
    val audioPlayMode = MutableStateFlow(MediaPlayMode.REPEAT)
    val audioPlaybackSpeed = MutableStateFlow(0f)
    val imageGridCellsPerRow = MutableStateFlow(0)
    val videoGridCellsPerRow = MutableStateFlow(0)
    val showHiddenFiles = MutableStateFlow(false)
    val feedAutoRefresh = MutableStateFlow(false)
    val feedAutoRefreshInterval = MutableStateFlow(0)
    val feedAutoRefreshOnlyWifi = MutableStateFlow(false)
    val feedFontScale = MutableStateFlow(0)
    val editorWrapContent = MutableStateFlow(false)
    val editorFontSize = MutableStateFlow(0)
    val editorStatusBar = MutableStateFlow(false)
    val nearbyDiscoverable = MutableStateFlow(false)
    val notificationFilter = MutableStateFlow(NotificationFilterData(NotificationFilterMode.BLACKLIST, emptySet()))
    val pomodoroSettings = MutableStateFlow(
        PomodoroSettings(
            workDurationMin = 0,
            shortBreakDurationMin = 0,
            longBreakDurationMin = 0,
            pomodorosBeforeLongBreak = 0,
            showNotification = false,
            playSoundOnComplete = false,
            soundPath = "",
            originalSoundName = "",
        )
    )
    val appHomeFeatures = MutableStateFlow("")
    val launcherShortcuts = MutableStateFlow("")
    val audioSortBy = MutableStateFlow(AudioSortBy.UPDATED_AT_ASC)
    val videoSortBy = MutableStateFlow(VideoSortBy.UPDATED_AT_ASC)
    val imageSortBy = MutableStateFlow(ImageSortBy.UPDATED_AT_ASC)
    val docSortBy = MutableStateFlow(DocSortBy.UPDATED_AT_ASC)
    val fileSortBy = MutableStateFlow(FileSortBy.UPDATED_AT_ASC)
    val packageSortBy = MutableStateFlow(FileSortBy.UPDATED_AT_ASC)
    val clientId = MutableStateFlow("")
    val deviceName = MutableStateFlow("")
    val mdnsHostname = MutableStateFlow("")
    val onboardingCompleted = MutableStateFlow(false)
    val screenMirrorQuality = MutableStateFlow(ScreenMirrorQuality(ScreenMirrorMode.HD, 0))

    internal fun load(values: JsonObject = nativeValue<JsonObject>(RustNativeBindings.loadPreferencesResponse())) {
        for ((key, value) in values) update(key, value)
    }

    internal suspend inline fun <reified T> set(key: String, value: T): Boolean = set(
        key, value, { name, encoded -> RustNativeApi.setPref(name, encoded) },
        { load() },
    )

    internal suspend inline fun <reified T> set(
        key: String,
        value: T,
        crossinline persist: suspend (String, JsonElement) -> Boolean,
        crossinline reload: () -> Unit,
    ): Boolean = persist(key, JsonHelper.jsonEncodeToElement(value)).also { reload() }

    internal fun update(key: String, value: JsonElement) {
        when (key) {
            "passwordConfigured" -> passwordConfigured.value = value.jsonPrimitive.boolean
            PrefKeys.PASSWORD_TYPE -> passwordType.value = PasswordType.valueOf(value.jsonPrimitive.content)
            PrefKeys.LOGIN_REQUIRE_APPROVAL -> loginRequireApproval.value = value.jsonPrimitive.boolean
            PrefKeys.API_PERMISSIONS -> apiAccessFeatures.value = JsonHelper.jsonDecodeFromElement(value)
            PrefKeys.UPDATE_INFO -> updateInfo.value = JsonHelper.jsonDecodeFromElement(value)
            PrefKeys.APP_UPDATE_AVAILABLE -> appUpdateAvailable.value = value.jsonPrimitive.boolean
            PrefKeys.AUTO_CHECK_UPDATE -> autoCheckUpdate.value = value.jsonPrimitive.boolean
            PrefKeys.ADB_TOKEN -> adbToken.value = value.jsonPrimitive.content
            PrefKeys.HTTP_PORT -> httpPort.value = value.jsonPrimitive.int
            PrefKeys.HTTPS_PORT -> httpsPort.value = value.jsonPrimitive.int
            PrefKeys.DARK_THEME -> darkTheme.value = DarkTheme.valueOf(value.jsonPrimitive.content)
            PrefKeys.AMOLED_DARK_THEME -> amoledDarkTheme.value = value.jsonPrimitive.boolean
            PrefKeys.PDF_FOLLOW_DARK_THEME -> pdfFollowDarkTheme.value = value.jsonPrimitive.boolean
            PrefKeys.LOCALE -> locale.value = value.jsonPrimitive.content
            PrefKeys.BACKGROUND_SERVICE_ENABLED -> backgroundServiceEnabled.value = value.jsonPrimitive.boolean
            PrefKeys.REMOTE_ACCESS -> remoteAccess.value = value.jsonPrimitive.boolean
            PrefKeys.DLNA -> dlna.value = value.jsonPrimitive.boolean
            PrefKeys.DEVELOPER_MODE -> developerMode.value = value.jsonPrimitive.boolean
            PrefKeys.ALLOW_ANY_HOST -> allowAnyHost.value = value.jsonPrimitive.boolean
            PrefKeys.HTTPS -> https.value = value.jsonPrimitive.boolean
            PrefKeys.WEB_ADDRESS_BAR_EXPANDED -> webAddressBarExpanded.value = value.jsonPrimitive.boolean
            PrefKeys.AUDIO_PLAY_MODE -> audioPlayMode.value = MediaPlayMode.valueOf(value.jsonPrimitive.content)
            PrefKeys.AUDIO_PLAYBACK_SPEED -> audioPlaybackSpeed.value = value.jsonPrimitive.float
            PrefKeys.IMAGE_GRID_CELLS_PER_ROW -> imageGridCellsPerRow.value = value.jsonPrimitive.int
            PrefKeys.VIDEO_GRID_CELLS_PER_ROW -> videoGridCellsPerRow.value = value.jsonPrimitive.int
            PrefKeys.SHOW_HIDDEN_FILES -> showHiddenFiles.value = value.jsonPrimitive.boolean
            PrefKeys.FEED_AUTO_REFRESH -> feedAutoRefresh.value = value.jsonPrimitive.boolean
            PrefKeys.FEED_AUTO_REFRESH_INTERVAL -> feedAutoRefreshInterval.value = value.jsonPrimitive.int
            PrefKeys.FEED_AUTO_REFRESH_ONLY_WIFI -> feedAutoRefreshOnlyWifi.value = value.jsonPrimitive.boolean
            PrefKeys.FEED_FONT_SCALE -> feedFontScale.value = value.jsonPrimitive.int
            PrefKeys.EDITOR_WRAP_CONTENT -> editorWrapContent.value = value.jsonPrimitive.boolean
            PrefKeys.EDITOR_FONT_SIZE -> editorFontSize.value = value.jsonPrimitive.int
            PrefKeys.EDITOR_STATUS_BAR -> editorStatusBar.value = value.jsonPrimitive.boolean
            PrefKeys.NEARBY_DISCOVERABLE -> nearbyDiscoverable.value = value.jsonPrimitive.boolean
            PrefKeys.NOTIFICATION_FILTER -> notificationFilter.value = JsonHelper.jsonDecodeFromElement(value)
            PrefKeys.POMODORO_SETTINGS -> pomodoroSettings.value = JsonHelper.jsonDecodeFromElement(value)
            PrefKeys.APP_HOME_FEATURES -> appHomeFeatures.value = value.jsonPrimitive.content
            PrefKeys.LAUNCHER_SHORTCUTS -> launcherShortcuts.value = value.jsonPrimitive.content
            PrefKeys.AUDIO_SORT_BY -> audioSortBy.value = AudioSortBy.valueOf(value.jsonPrimitive.content)
            PrefKeys.VIDEO_SORT_BY -> videoSortBy.value = VideoSortBy.valueOf(value.jsonPrimitive.content)
            PrefKeys.IMAGE_SORT_BY -> imageSortBy.value = ImageSortBy.valueOf(value.jsonPrimitive.content)
            PrefKeys.DOC_SORT_BY -> docSortBy.value = DocSortBy.valueOf(value.jsonPrimitive.content)
            PrefKeys.FILE_SORT_BY -> fileSortBy.value = FileSortBy.valueOf(value.jsonPrimitive.content)
            PrefKeys.PACKAGE_SORT_BY -> packageSortBy.value = FileSortBy.valueOf(value.jsonPrimitive.content)
            PrefKeys.CLIENT_ID -> clientId.value = value.jsonPrimitive.content
            PrefKeys.DEVICE_NAME -> deviceName.value = value.jsonPrimitive.content
            PrefKeys.MDNS_HOSTNAME -> mdnsHostname.value = value.jsonPrimitive.content
            PrefKeys.ONBOARDING_COMPLETED -> onboardingCompleted.value = value.jsonPrimitive.boolean
            PrefKeys.SCREEN_MIRROR_QUALITY -> screenMirrorQuality.value = JsonHelper.jsonDecodeFromElement(value)
        }
    }

    fun localeValue(): Locale? = parseLocale(locale.value)

    fun parseLocale(value: String): Locale? =
        value.takeIf { it.isNotEmpty() }?.split('-')?.let { Locale(it[0], it.getOrElse(1) { "" }) }

    fun formatLocale(value: Locale?): String =
        value?.let { it.language + if (it.country.isEmpty()) "" else "-${it.country}" }.orEmpty()

    suspend fun toggleNotificationApp(packageName: String) =
        RustNativeApi.toggleNotificationApp(packageName)

    suspend fun setNotificationMode(mode: NotificationFilterMode) = RustNativeApi.setNotificationFilterMode(mode)

    suspend fun clearNotificationApps() = RustNativeApi.clearNotificationFilterApps()

    fun parseFeatures(value: String): List<String> = value.split('|').filter { it.isNotBlank() }

    fun formatFeatures(value: List<String>): String = value.joinToString("|")

    fun selectedLauncherShortcuts(value: String): List<AppFeatureType> =
        parseFeatures(value).mapNotNull { name -> AppFeatureType.entries.find { it.name == name } }

    fun formatLauncherShortcuts(value: List<AppFeatureType>): String =
        value.joinToString("|") { it.name }

    suspend fun setPassword(password: String) = set(PrefKeys.PASSWORD, password)

    suspend fun setPasswordType(value: PasswordType) = set(PrefKeys.PASSWORD_TYPE, value.name)

    suspend fun setLoginRequireApproval(enabled: Boolean) = set(PrefKeys.LOGIN_REQUIRE_APPROVAL, enabled)


    suspend fun generatePassword() = RustNativeApi.generatePassword()


    suspend fun resetAdbToken() = RustNativeApi.resetAdbToken()

    suspend fun setApiAccess(
        features: Set<com.ismartcoding.plain.features.web.AccessFeature>,
        enabled: Boolean,
    ) = set(PrefKeys.API_PERMISSIONS, features.associateWith { enabled })

    suspend fun setApiAccessFeatures(features: Set<com.ismartcoding.plain.features.web.AccessFeature>) = set(PrefKeys.API_PERMISSIONS, features)

    suspend fun completeOnboarding() = RustNativeApi.completeOnboarding()

    suspend fun setMdnsHostname(hostname: String) = set(PrefKeys.MDNS_HOSTNAME, hostname)

    suspend fun setAutoCheckUpdate(value: Boolean) = set(PrefKeys.AUTO_CHECK_UPDATE, value)

    suspend fun setSkipVersion(value: String) = set(PrefKeys.SKIP_VERSION, value)

    suspend fun verifyAdbToken(token: String): Boolean = RustNativeApi.verifyAdbToken(token)
}
