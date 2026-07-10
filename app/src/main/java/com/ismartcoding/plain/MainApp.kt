package com.ismartcoding.plain

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import coil3.SingletonImageLoader
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.lib.isUPlus
import com.ismartcoding.plain.lib.logcat.DiskLogAdapter
import com.ismartcoding.plain.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.events.PowerConnectedEvent
import com.ismartcoding.plain.events.AppEvents
import com.ismartcoding.plain.events.StartNearbyServiceEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preferences.AdbTokenPreference
import com.ismartcoding.plain.preferences.AudioPlayModePreference
import com.ismartcoding.plain.preferences.UpdateInfoPreference
import com.ismartcoding.plain.preferences.ClientIdPreference
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.preferences.DarkThemePreference
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.preferences.FeedAutoRefreshPreference
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.preferences.HttpsPreference
import com.ismartcoding.plain.preferences.KeyStorePasswordPreference
import com.ismartcoding.plain.preferences.MdnsHostnamePreference
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.preferences.SignatureKeyPreference
import com.ismartcoding.plain.preferences.UrlTokenPreference
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.preferences.getPreferencesAsync
import com.ismartcoding.plain.preferences.initDataStore
import com.ismartcoding.plain.preferences.FidUriExtMigratedPreference
import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.ui.base.coil.newImageLoader
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.workers.FeedFetchWorker
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DataInitializer
import com.ismartcoding.plain.db.Migrations
import com.ismartcoding.plain.db.initDatabase
import com.ismartcoding.plain.helpers.ChatFidUriMigration
import com.ismartcoding.plain.lib.isQPlus
import com.ismartcoding.plain.preferences.ensureKeyPairAsync
import com.ismartcoding.plain.preferences.ensureValueAsync
import com.ismartcoding.plain.preferences.setDarkMode
import dalvik.system.ZipPathValidator
import kotlin.time.Duration.Companion.days

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()

        instance = this
        com.ismartcoding.plain.setAppContext(this)
        com.ismartcoding.plain.db.setAppContext(this)
        com.ismartcoding.plain.features.locale.setAppContext(this)
        initDataStore(dataStore)
        initDatabase(
            com.ismartcoding.plain.db.buildAppDatabase(Constants.DATABASE_NAME)
                .addCallback(object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        DataInitializer(this@MainApp, db).apply {
                            insertWelcome()
                            insertTags()
                            insertNotes()
                        }
                    }
                })
                .build()
        )

        CrashHandler.install(this)

        SingletonImageLoader.setSafe { context ->
            newImageLoader(context)
        }

        LogCat.init(this)
        LogCat.addLogAdapter(
            DiskLogAdapter(
                DiskLogFormatStrategy.getInstance(),
                minPriority = if (BuildConfig.DEBUG) LogCat.VERBOSE else LogCat.WARN,
            ),
        )

        com.ismartcoding.plain.api.httpLogSink = com.ismartcoding.plain.api.HttpLogSink { LogCat.v(it) }

        AppEvents.register()
        HttpServerManager.warmUp()
        NetworkMonitor.init(this)
        if (isQPlus()) {
            try {
                audioManager.allowedCapturePolicy = AudioAttributes.ALLOW_CAPTURE_BY_ALL
            } catch (_: Exception) {
            }
        }

        // https://stackoverflow.com/questions/77683434/the-getnextentry-method-of-zipinputstream-throws-a-zipexception-invalid-zip-ent
        if (isUPlus()) {
            ZipPathValidator.clearCallback()
        }

        // Disable Smart Text Selection to avoid framework crash in SmartSelectSprite
        try {
            val manager = getSystemService(TextClassificationManager::class.java)
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
            TempData.deviceName.value = DeviceNamePreference.get(preferences).ifEmpty { PhoneHelper.getDeviceName(instance) }
            KeyStorePasswordPreference.ensureValueAsync(preferences)
            UrlTokenPreference.ensureValueAsync(preferences)
            SignatureKeyPreference.ensureKeyPairAsync()
            MdnsHostnamePreference.ensureValueAsync(preferences)

            DarkThemePreference.setDarkMode(DarkTheme.parse(DarkThemePreference.get(preferences)))
            if (TempData.webEnabled.value && PlugInControlReceiver.isUSBConnected(this@MainApp)) {
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
                ChatFidUriMigration.run(this@MainApp)
                FidUriExtMigratedPreference.putAsync(true)
            }
            if (FeedAutoRefreshPreference.get(preferences)) {
                FeedFetchWorker.startRepeatWorkerAsync(instance)
            }
            // Start Nearby service (always listen regardless of discoverable setting)
            sendEvent(StartNearbyServiceEvent())
            HttpServerManager.clientTsInterval()
            ImageSearchManager.restoreIfEnabled()
            val thirtyDaysAgo = (kotlin.time.Clock.System.now() - 30.days).toString()
            AppDatabase.instance.videoPlayProgressDao().getRecentProgress(thirtyDaysAgo).forEach {
                TempData.videoPlayProgressMap[it.mediaId] = it.duration
            }
            if (AppFeatureType.CHECK_UPDATES.has() && autoCheckUpdate && checkUpdateTime < System.currentTimeMillis() - Constants.ONE_DAY_MS) {
                AppHelper.checkUpdateAsync(this@MainApp, false)
            }
        }
    }

    companion object {
        lateinit var instance: MainApp

        fun getAppVersion(): String {
            return BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
        }

        fun getAndroidVersion(): String {
            return Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")"
        }
    }
}
