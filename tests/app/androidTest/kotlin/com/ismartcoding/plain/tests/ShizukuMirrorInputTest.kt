package com.ismartcoding.plain.tests

import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.features.screenmirror.ScreenMirrorControlAction.*
import com.ismartcoding.plain.features.screenmirror.ScreenMirrorControlInput
import com.ismartcoding.plain.platform.media.dispatchScreenMirrorControls
import com.ismartcoding.plain.platform.media.isScreenMirrorRunning
import com.ismartcoding.plain.platform.system.ShizukuHelper
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.services.PlainAccessibilityService
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class ShizukuMirrorInputTest {
    @Serializable private data class Controls(val inputs: List<ScreenMirrorControlInput>)

    @Test fun authorizedInputReachesTheMirroredActivity() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("mirrorConsentTest") == "true")
        val shizuku = InstrumentationRegistry.getArguments().getString("mirrorBackend") == "shizuku"
        assertEquals(shizuku, ShizukuHelper.isGranted())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            if (!shizuku) withTimeout(30_000) { while (!PlainAccessibilityService.isEnabled()) delay(100) }
            assertEquals(!shizuku, PlainAccessibilityService.isEnabled())
            val events = LinkedBlockingQueue<Pair<Int, Int>>()
            scenario.onActivity { activity ->
                val target = View(activity)
                target.setOnTouchListener { _, event ->
                    events.offer(event.actionMasked to event.pointerCount)
                    true
                }
                activity.setContentView(target)
            }
            GraphqlApi.execute(GraphqlRequest<Unit>("mutation { startScreenMirror(audio:false) }"))
            try {
                withTimeout(90_000) {
                    var ready = false
                    while (!ready) {
                        scenario.onActivity {
                            ready = it.hasWindowFocus() && isScreenMirrorRunning() &&
                                (shizuku || PlainAccessibilityService.isEnabled())
                        }
                        if (!ready) delay(100)
                    }
                }
                val inputs = if (shizuku) listOf(
                    ScreenMirrorControlInput(TOUCH_DOWN, 0.4f, 0.5f, pointerId = 80),
                    ScreenMirrorControlInput(TOUCH_DOWN, 0.6f, 0.5f, pointerId = 81),
                    ScreenMirrorControlInput(TOUCH_MOVE, 0.7f, 0.5f, pointerId = 81),
                    ScreenMirrorControlInput(TOUCH_UP, 0.7f, 0.5f, pointerId = 81),
                    ScreenMirrorControlInput(TOUCH_UP, 0.4f, 0.5f, pointerId = 80),
                ) else listOf(ScreenMirrorControlInput(TAP, 0.5f, 0.5f))
                dispatchScreenMirrorControls(JsonHelper.jsonEncodeToElement(Controls(inputs)).jsonObject)
                val expected = if (shizuku) listOf(MotionEvent.ACTION_DOWN to 1, MotionEvent.ACTION_POINTER_DOWN to 2,
                    MotionEvent.ACTION_MOVE to 2, MotionEvent.ACTION_POINTER_UP to 2, MotionEvent.ACTION_UP to 1)
                else listOf(MotionEvent.ACTION_DOWN to 1, MotionEvent.ACTION_UP to 1)
                for (sample in expected) {
                    assertEquals(sample, events.poll(5, TimeUnit.SECONDS))
                }
            } finally {
                GraphqlApi.execute(GraphqlRequest<Unit>("mutation { stopScreenMirror }"))
            }
        }
    }
}
