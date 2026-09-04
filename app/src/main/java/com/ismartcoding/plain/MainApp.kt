package com.ismartcoding.plain

import android.app.Application
import android.content.ComponentCallbacks2
import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.platform.clearImageMemoryCache

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Bound the HTTP server's buffer memory BEFORE any Netty class loads
        // (must precede MainAppHelper.init -> warmUpNetty).
        //
        // 1. Prefer HEAP buffers: Netty's default allocator prefers direct
        //    (off-heap) buffers with a ceiling of Runtime.maxMemory() — a
        //    whole extra memory budget on top of the heap. In memory-sandboxed
        //    environments (Huawei Zhuoyitong container) that double budget
        //    gets the process killed when several video streams ramp the
        //    pool. With heap preference every buffer lives inside the Java
        //    heap budget; socket writes with heap buffers cost one extra
        //    memcpy per 64KiB chunk, immeasurable next to network time.
        // 2. Cap direct memory as a safety net for any explicit
        //    directBuffer() caller.
        System.setProperty("io.netty.noPreferDirect", "true")
        System.setProperty("io.netty.maxDirectMemory", (32L * 1024 * 1024).toString())
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
