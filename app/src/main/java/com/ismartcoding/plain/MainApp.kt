package com.ismartcoding.plain

import android.app.Application
import android.os.Build
import com.ismartcoding.lib.brv.utils.BRV
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.DiskLogAdapter
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.data.preference.PasswordTypePreference
import com.ismartcoding.plain.features.AppEvents
import com.ismartcoding.plain.features.bluetooth.BluetoothEvents
import com.ismartcoding.plain.features.box.BoxEvents
import com.ismartcoding.plain.ui.helpers.PageHelper
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.workers.FeedFetchWorker
import com.tencent.mmkv.MMKV
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainApp : Application() {
    var httpServer: NettyApplicationEngine? = null
    var httpServerError: String = ""
    val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        instance = this

        LogCat.addLogAdapter(DiskLogAdapter(DiskLogFormatStrategy.getInstance(this)))

        MMKV.initialize(this)

        LocalStorage.init()

        BRV.modelId = BR.m

        PageHelper.init()

        BluetoothEvents.register()
        AppEvents.register()
        BoxEvents.register()

        coIO {
            if (PasswordTypePreference.get(instance) == PasswordType.RANDOM.value) {
                HttpServerManager.resetPassword()
            }
            HttpServerManager.loadTokenCache()
            if (LocalStorage.feedAutoRefresh) {
                FeedFetchWorker.startRepeatWorker()
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
