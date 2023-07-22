package com.ismartcoding.plain

import android.app.Application
import android.os.Build
import com.ismartcoding.lib.brv.utils.BRV
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.DiskLogAdapter
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.enums.DarkTheme
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.data.preference.ClientIdPreference
import com.ismartcoding.plain.data.preference.DarkThemePreference
import com.ismartcoding.plain.data.preference.FeedAutoRefreshPreference
import com.ismartcoding.plain.data.preference.KeyStorePasswordPreference
import com.ismartcoding.plain.data.preference.PasswordTypePreference
import com.ismartcoding.plain.features.AppEvents
import com.ismartcoding.plain.features.bluetooth.BluetoothEvents
import com.ismartcoding.plain.features.box.BoxEvents
import com.ismartcoding.plain.ui.helpers.PageHelper
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.workers.FeedFetchWorker
import io.ktor.server.netty.NettyApplicationEngine

class MainApp : Application() {
    var httpServer: NettyApplicationEngine? = null

    override fun onCreate() {
        super.onCreate()

        instance = this

        LogCat.addLogAdapter(DiskLogAdapter(DiskLogFormatStrategy.getInstance(this)))

        ClientIdPreference.ensureValue(this)
        KeyStorePasswordPreference.ensureValue(this)

        BRV.modelId = BR.m

        PageHelper.init()

        BluetoothEvents.register()
        AppEvents.register()
        BoxEvents.register()

        coIO {
            DarkThemePreference.setDarkMode(DarkTheme.parse(DarkThemePreference.get(instance)))

            if (PasswordTypePreference.getValue(instance) == PasswordType.RANDOM) {
                HttpServerManager.resetPassword()
            }
            HttpServerManager.loadTokenCache()
            if (FeedAutoRefreshPreference.get(instance)) {
                FeedFetchWorker.startRepeatWorker(instance)
            }
            HttpServerManager.clientTsInterval()
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
