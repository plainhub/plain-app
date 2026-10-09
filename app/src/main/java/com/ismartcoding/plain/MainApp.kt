package com.ismartcoding.plain

import android.app.Application
import android.content.ComponentCallbacks2
import com.ismartcoding.plain.platform.clearImageMemoryCache

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setAppContext(this, buildChannel = BuildConfig.CHANNEL)
        MainAppHelper.init(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            clearImageMemoryCache()
        }
    }
}
