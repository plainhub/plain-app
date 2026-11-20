package com.ismartcoding.plain.tests

import android.app.ActivityManager
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.services.HttpServerForegroundService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundServiceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun openingAppKeepsServiceInForegroundAfterUiStops() {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        fun isInForeground(): Boolean = activityManager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == HttpServerForegroundService::class.java.name && it.foreground
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val deadline = SystemClock.elapsedRealtime() + 5_000
            while (!isInForeground() && SystemClock.elapsedRealtime() < deadline) {
                SystemClock.sleep(50)
            }
            assertTrue("Opening PlainApp should start its foreground service", isInForeground())
            scenario.onActivity { assertTrue(it.moveTaskToBack(true)) }
            val stopDeadline = SystemClock.elapsedRealtime() + 5_000
            while (scenario.state != Lifecycle.State.CREATED && SystemClock.elapsedRealtime() < stopDeadline) {
                SystemClock.sleep(50)
            }
            assertEquals(Lifecycle.State.CREATED, scenario.state)
            assertTrue("Stopping the activity should leave the service in the foreground", isInForeground())

            val notifications = context.getSystemService(NotificationManager::class.java)
            if (notifications.areNotificationsEnabled()) {
                val notification = notifications.activeNotifications.single {
                    it.id == HttpServerForegroundService.notificationId
                }.notification
                assertTrue("The foreground service notification should have no actions", notification.actions.isNullOrEmpty())
            }
        }
    }

    @Test
    fun openingAppRequestsNotificationsAndPostsForegroundNotificationAfterGrant() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val notifications = context.getSystemService(NotificationManager::class.java)
        assumeFalse(notifications.areNotificationsEnabled())

        ActivityScenario.launch(MainActivity::class.java).use {
            fun permissionButton(): AccessibilityNodeInfo? {
                return instrumentation.uiAutomation.rootInActiveWindow
                    ?.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/permission_allow_button")
                    ?.firstOrNull()
            }

            val promptDeadline = SystemClock.elapsedRealtime() + 5_000
            while (permissionButton() == null && SystemClock.elapsedRealtime() < promptDeadline) {
                SystemClock.sleep(50)
            }
            val allow = checkNotNull(permissionButton()) { "Opening PlainApp should request notification permission" }
            assertTrue(allow.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            val notificationDeadline = SystemClock.elapsedRealtime() + 5_000
            fun serviceNotification() = notifications.activeNotifications.singleOrNull {
                it.id == HttpServerForegroundService.notificationId
            }?.notification
            while (serviceNotification() == null && SystemClock.elapsedRealtime() < notificationDeadline) {
                SystemClock.sleep(50)
            }
            val notification = checkNotNull(serviceNotification()) { "Granting permission should post the foreground service notification" }
            assertTrue(notification.flags and android.app.Notification.FLAG_FOREGROUND_SERVICE != 0)
            assertTrue(notification.actions.isNullOrEmpty())
        }
    }

    @Test
    fun settingsLeavesSixteenDpBetweenWarningAndRemoteAccessCard() {
        assumeFalse(Settings.canDrawOverlays(context))
        val onboardingCompleted = Prefs.onboardingCompleted.value
        val rootTab = MainViewModel.currentRootTab.intValue
        try {
            instrumentation.runOnMainSync {
                Prefs.onboardingCompleted.value = true
                MainViewModel.currentRootTab.intValue = 0
            }
            ActivityScenario.launch(MainActivity::class.java).use {
                val deadline = SystemClock.elapsedRealtime() + 10_000
                var notificationWarningVisible = false
                var gap: Float? = null
                while (gap == null && SystemClock.elapsedRealtime() < deadline) {
                    instrumentation.runOnMainSync {
                        val nodes = WindowInspector.getGlobalWindowViews().filter(View::isShown)
                            .flatMap(::composeRoots)
                            .flatMap { flatten(it.semanticsOwner.unmergedRootSemanticsNode) }
                        notificationWarningVisible = nodes.any { it.config.getOrNull(SemanticsProperties.TestTag) == "settings-notification-warning" }
                        val alert = nodes.firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == "settings-overlay-warning" }
                        val card = nodes.firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == "settings-remote-access" }
                        if (alert != null && card != null) gap = card.boundsInRoot.top - alert.boundsInRoot.bottom
                    }
                    if (gap == null) SystemClock.sleep(50)
                }
                assertEquals("Settings should prompt when notifications are disabled",
                    !context.getSystemService(NotificationManager::class.java).areNotificationsEnabled(), notificationWarningVisible)
                val density = context.resources.displayMetrics.density
                assertEquals(16f * density, checkNotNull(gap) { "Settings warning and Remote Access card must be visible" }, 1f)
            }
        } finally {
            instrumentation.runOnMainSync {
                Prefs.onboardingCompleted.value = onboardingCompleted
                MainViewModel.currentRootTab.intValue = rootTab
            }
        }
    }

    private fun composeRoots(view: View): List<ViewRootForTest> = when (view) {
        is ViewRootForTest -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { composeRoots(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun flatten(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::flatten)
}
