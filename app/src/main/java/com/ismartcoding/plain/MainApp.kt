package com.ismartcoding.plain

import android.app.Application
import android.content.ComponentCallbacks2
import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.platform.clearImageMemoryCache

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setAppContext(this, buildChannel = BuildConfig.CHANNEL)
        MainAppHelper.init(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Play's bitmap-memory metric penalizes holding bitmaps in non-visible
        // states; the disk cache restores them instantly when the UI returns.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            clearImageMemoryCache()
        }
        // Real memory pressure while running: drop the resident AI models too;
        // they reload lazily from disk on the next search.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            ImageSearchManager.releaseModels()
        }
    }
}
