package com.ismartcoding.plain

import android.app.Application
import android.media.AudioAttributes
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import coil3.SingletonImageLoader
import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DataInitializer
import com.ismartcoding.plain.platform.buildAppDatabase
import com.ismartcoding.plain.platform.initDatabase
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.events.AppEvents
import com.ismartcoding.plain.events.PowerConnectedEvent
import com.ismartcoding.plain.events.StartNearbyServiceEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.ChatFidUriMigration
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.platform.isUPlus
import com.ismartcoding.plain.lib.logcat.DiskLogAdapter
import com.ismartcoding.plain.platform.DiskLogFormatStrategy
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.preferences.AdbTokenPreference
import com.ismartcoding.plain.preferences.AudioPlayModePreference
import com.ismartcoding.plain.preferences.ClientIdPreference
import com.ismartcoding.plain.preferences.DarkThemePreference
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.preferences.FeedAutoRefreshPreference
import com.ismartcoding.plain.preferences.FidUriExtMigratedPreference
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.preferences.HttpsPreference
import com.ismartcoding.plain.preferences.KeyStorePasswordPreference
import com.ismartcoding.plain.preferences.MdnsHostnamePreference
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.preferences.SignatureKeyPreference
import com.ismartcoding.plain.preferences.UpdateInfoPreference
import com.ismartcoding.plain.preferences.UrlTokenPreference
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.preferences.ensureKeyPairAsync
import com.ismartcoding.plain.preferences.ensureValueAsync
import com.ismartcoding.plain.preferences.getPreferencesAsync
import com.ismartcoding.plain.preferences.initDataStore
import com.ismartcoding.plain.preferences.setDarkMode
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.platform.newImageLoader
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.webserver.warmUpNetty
import com.ismartcoding.plain.workers.FeedFetchWorker
import dalvik.system.ZipPathValidator
import kotlin.time.Duration.Companion.days

object MainAppHelper {

    fun init(app: Application) {
        com.ismartcoding.plain.platform.setLocaleContext(app)
        com.ismartcoding.plain.thumbnail.ThumbnailProvider.instance = com.ismartcoding.plain.thumbnail.ThumbnailGenerator
        initDataStore(app.dataStore)
        initDatabase(
            buildAppDatabase(Constants.DATABASE_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(connection: SQLiteConnection) {
                        DataInitializer(connection).apply {
                            insertWelcome()
                            insertTags()
                            insertNotes()
                        }
                    }
                })
                .build()
        )

        CrashHandler.install(app)

        SingletonImageLoader.setSafe { context -> newImageLoader(context) }

        LogCat.init(app)
        LogCat.addLogAdapter(
            DiskLogAdapter(
                DiskLogFormatStrategy.getInstance(),
                minPriority = if (isDebugBuild()) LogCat.VERBOSE else LogCat.WARN,
            ),
        )

        com.ismartcoding.plain.api.httpLogSink = com.ismartcoding.plain.api.HttpLogSink { LogCat.v(it) }

        AppEvents.register()
        warmUpNetty()
        NetworkMonitor.init(app)
        if (isQPlus()) {
            try {
                audioManager.allowedCapturePolicy = AudioAttributes.ALLOW_CAPTURE_BY_ALL
            } catch (_: Exception) {
            }
        }

        if (isUPlus()) {
            ZipPathValidator.clearCallback()
        }

        try {
            val manager = app.getSystemService(TextClassificationManager::class.java)
            manager?.setTextClassifier(TextClassifier.NO_OP)
        } catch (_: Throwable) {
        }

        coIO {
            val preferences = getPreferencesAsync()
            TempData.webEnabled.value = WebPreference.get(preferences)
            TempData.webHttps.value = HttpsPreference.get(preferences)
            TempData.httpPort.value = HttpPortPreference.get(preferences)
            TempData.httpsPort.value = HttpsPortPreference.get(preferences)
            TempData.audioPlayMode.value = AudioPlayModePreference.getValue(preferences)
            AdbTokenPreference.ensureValueAsync(preferences)
            TempData.nearbyDiscoverable = NearbyDiscoverablePreference.getAsync()
            val updateInfo = UpdateInfoPreference.getValueAsync()
            val checkUpdateTime = updateInfo.checkUpdateTime
            val autoCheckUpdate = updateInfo.autoCheckUpdate
            ClientIdPreference.ensureValueAsync(preferences)
            TempData.deviceName.value = DeviceNamePreference.get(preferences).ifEmpty { PhoneHelper.getDeviceName(app) }
            KeyStorePasswordPreference.ensureValueAsync(preferences)
            UrlTokenPreference.ensureValueAsync(preferences)
            SignatureKeyPreference.ensureKeyPairAsync()
            MdnsHostnamePreference.ensureValueAsync(preferences)

            DarkThemePreference.setDarkMode(DarkTheme.parse(DarkThemePreference.get(preferences)))
            if (TempData.webEnabled.value && PlugInControlReceiver.isUSBConnected(app)) {
                sendEvent(PowerConnectedEvent())
            }
            if (PasswordPreference.get(preferences).isEmpty()) {
                HttpServerManager.resetPasswordAsync()
            }
            HttpServerManager.loadTokenCache()
            PeerCacher.load()
            ChannelCacher.load()
            ChatCacher.load()
            if (!FidUriExtMigratedPreference.get(preferences)) {
                ChatFidUriMigration.run(app)
                FidUriExtMigratedPreference.putAsync(true)
            }
            if (FeedAutoRefreshPreference.get(preferences)) {
                FeedFetchWorker.startRepeatWorkerAsync(app)
            }
            sendEvent(StartNearbyServiceEvent())
            HttpServerManager.clientTsInterval()
            ImageSearchManager.restoreIfEnabled()
            val thirtyDaysAgo = (kotlin.time.Clock.System.now() - 30.days).toString()
            AppDatabase.instance.videoPlayProgressDao().getRecentProgress(thirtyDaysAgo).forEach {
                TempData.videoPlayProgressMap[it.mediaId] = it.duration
            }
            if (AppFeatureType.CHECK_UPDATES.has() && autoCheckUpdate && checkUpdateTime < System.currentTimeMillis() - Constants.ONE_DAY_MS) {
                AppHelper.checkUpdateAsync(app, false)
            }
        }
    }
}
