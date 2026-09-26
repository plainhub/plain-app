package com.ismartcoding.plain.services

import android.os.Handler
import android.os.HandlerThread

/**
 * Dedicated injection thread shared by the touch injectors: stream state,
 * gesture callbacks and hold-refresh timers all run here, off the main thread.
 */
internal object TouchInjectThread {
    @Volatile
    private var thread: HandlerThread? = null

    @Volatile
    private var handlerRef: Handler? = null

    /** The injection handler, starting the thread lazily on first use. */
    val handler: Handler
        get() {
            handlerRef?.let { return it }
            synchronized(this) {
                handlerRef?.let { return it }
                val t = HandlerThread("mirror-touch-inject").apply { start() }
                val h = Handler(t.looper)
                thread = t
                handlerRef = h
                return h
            }
        }

    /** True when the thread is alive (never starts it). */
    val isAlive: Boolean
        get() = handlerRef != null

    /** Schedule on the injection thread; false (no-op) when already shut down. */
    fun postDelayed(r: Runnable, delayMs: Long): Boolean {
        val h = handlerRef ?: return false
        return h.postDelayed(r, delayMs)
    }

    fun removeCallbacks(r: Runnable) {
        handlerRef?.removeCallbacks(r)
    }

    fun shutdown() {
        val t = thread
        thread = null
        handlerRef = null
        t?.quitSafely()
    }
}
