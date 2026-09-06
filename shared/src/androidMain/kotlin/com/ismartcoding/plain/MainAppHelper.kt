package com.ismartcoding.plain

import android.app.Application
import android.media.AudioAttributes
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import coil3.SingletonImageLoader
import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DataInitializer
import com.ismartcoding.plain.platform.buildAppDatabase
import com.ismartcoding.plain.platform.initDatabase
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.events.AppEvents
import com.ismartcoding.plain.events.PowerConnectedEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.ChatFidUriMigration
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.platform.isUPlus
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.initDiskLogging
import com.ismartcoding.plain.preferences.AdbTokenPreference
import com.ismartcoding.plain.preferences.DarkThemePreference
import com.ismartcoding.plain.preferences.FeedAutoRefreshPreference
import com.ismartcoding.plain.preferences.FidUriExtMigratedPreference
import com.ismartcoding.plain.preferences.UpdateInfoPreference
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.preferences.ensureValueAsync
import com.ismartcoding.plain.preferences.initDataStore
import com.ismartcoding.plain.preferences.setDarkMode
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.platform.newImageLoader
import com.ismartcoding.plain.httpserver.warmUpHttpServer
import com.ismartcoding.plain.workers.FeedFetchWorker
import dalvik.system.ZipPathValidator
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

object MainAppHelper {

    fun init(app: Application) {
        com.ismartcoding.plain.thumbnail.ThumbnailProvider.instance = com.ismartcoding.plain.thumbnail.ThumbnailGenerator
        initDataStore(app.dataStore)
        initDatabase(
            buildAppDatabase(Constants.DATABASE_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override suspend fun onCreate(connection: SQLiteConnection) {
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

        // Disk logging + HTTP request logging (debug=VERBOSE, release=WARN)
        initDiskLogging()

        AppEvents.register()
        warmUpHttpServer()
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
            // Load media duration cache first — needed by VideoMediaStoreHelper
            // before the first list query. fMP4 videos have MediaStore.DURATION=0
            // and rely on this cache; loading it last caused a race where the
            // Videos page showed duration=0 on quick app open.
            AppDatabase.instance.mediaItemDao().getAll().forEach {
                TempData.mediaDurationMap["${it.mediaType}:${it.mediaId}"] = it.duration
            }

            val preferences = initCommonPreferences()
            DarkThemePreference.setDarkMode(DarkTheme.parse(DarkThemePreference.get(preferences)))
            AdbTokenPreference.ensureValueAsync(preferences)
            if (TempData.serviceEnabled.value && PlugInControlReceiver.isUSBConnected(app)) {
                sendEvent(PowerConnectedEvent())
            }

            if (!FidUriExtMigratedPreference.get(preferences)) {
                ChatFidUriMigration.run(app)
                FidUriExtMigratedPreference.putAsync(true)
            }
            if (FeedAutoRefreshPreference.get(preferences)) {
                FeedFetchWorker.startRepeatWorkerAsync(app)
            }
            ImageSearchManager.restoreIfEnabled()
            val thirtyDaysAgo = (Clock.System.now() - 30.days).toString()
            AppDatabase.instance.videoPlayProgressDao().getRecentProgress(thirtyDaysAgo).forEach {
                TempData.videoPlayProgressMap[it.mediaId] = it.duration
            }

            val updateInfo = UpdateInfoPreference.getValueAsync()
            val checkUpdateTime = updateInfo.checkUpdateTime
            val autoCheckUpdate = updateInfo.autoCheckUpdate
            if (AppFeatureType.CHECK_UPDATES.has() && autoCheckUpdate && checkUpdateTime < System.currentTimeMillis() - Constants.ONE_DAY_MS) {
                AppHelper.checkUpdateAsync(app, false)
            }
        }
    }
}
