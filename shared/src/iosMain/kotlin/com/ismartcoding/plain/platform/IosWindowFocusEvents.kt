package com.ismartcoding.plain.platform

import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.lib.sendEvent
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

/**
 * Registers observers for iOS app focus changes so that commonMain code
 * can react to the app gaining/losing window focus via [WindowFocusChangedEvent].
 *
 * On iOS there is no exact equivalent of Android's onWindowFocusChanged; the
 * closest mapping is UIApplication's active/in-active transitions:
 * - didBecomeActive    → hasFocus = true  (app is in the foreground and receiving events)
 * - willResignActive   → hasFocus = false (app is about to lose foreground status, e.g.
 *                        notification shade, phone call, switching apps)
 */
object IosWindowFocusEvents {
    private var registered = false

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    fun register() {
        if (registered) return
        registered = true

        val center = NSNotificationCenter.defaultCenter

        center.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null,
        ) { _ ->
            sendEvent(WindowFocusChangedEvent(hasFocus = true))
        }

        center.addObserverForName(
            UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = null,
        ) { _ ->
            sendEvent(WindowFocusChangedEvent(hasFocus = false))
        }
    }
}
