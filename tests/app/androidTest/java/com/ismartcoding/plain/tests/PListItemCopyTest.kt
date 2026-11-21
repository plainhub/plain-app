package com.ismartcoding.plain.tests

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.resources.Res
import com.ismartcoding.plain.ui.resources.copy_text
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PListItemCopyTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test
    fun longPressCopiesTheWholeValueWithoutOpeningTheRow() {
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        check(InstrumentationRegistry.getArguments().getString("plainUiFixture") == "isolated")
        val value = "Full value: sample address"
        val taps = AtomicInteger()
        val copy = runBlocking { getString(Res.string.copy_text) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContentView(ComposeView(activity).apply {
                    setContent {
                        MaterialTheme {
                            PListItem(
                                modifier = Modifier.fillMaxWidth().clickable { taps.incrementAndGet() },
                                title = "IP",
                                value = value,
                            )
                        }
                    }
                })
            }
            val bounds = Rect().also { findNode(value).getBoundsInScreen(it) }
            val edge = 24 * instrumentation.targetContext.resources.displayMetrics.density
            val x = bounds.right - edge
            val y = bounds.centerY().toFloat()
            press(x, y, 0L)
            instrumentation.waitForIdleSync()
            assertEquals("A short tap should still activate the row", 1, taps.get())

            press(x, y, ViewConfiguration.getLongPressTimeout().toLong() + 150)
            val copyAction = findNode(copy, exact = true)
            assertTrue(copyAction.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                assertEquals(value, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
            }
            assertEquals("Long press must not activate the row", 1, taps.get())
        }
    }

    private fun press(x: Float, y: Float, hold: Long) {
        val down = SystemClock.uptimeMillis()
        fun send(action: Int) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
            try {
                instrumentation.sendPointerSync(event)
            } finally {
                event.recycle()
            }
        }
        send(MotionEvent.ACTION_DOWN)
        SystemClock.sleep(hold)
        send(MotionEvent.ACTION_UP)
    }

    private fun findNode(label: String, exact: Boolean = false): AccessibilityNodeInfo {
        fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            val text = node.text?.toString().orEmpty()
            if (node.isVisibleToUser && (if (exact) text == label else text.contains(label))) return node
            return (0 until node.childCount).firstNotNullOfOrNull { index -> node.getChild(index)?.let(::find) }
        }
        repeat(60) {
            instrumentation.uiAutomation.rootInActiveWindow?.let { find(it)?.let { node -> return node } }
            SystemClock.sleep(100)
        }
        error("Control not visible: $label")
    }
}
