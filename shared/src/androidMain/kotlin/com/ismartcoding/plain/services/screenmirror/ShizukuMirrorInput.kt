package com.ismartcoding.plain.services.screenmirror

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.Keep
import com.ismartcoding.plain.features.screenmirror.ScreenMirrorControlAction.*
import com.ismartcoding.plain.features.screenmirror.ScreenMirrorControlInput
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.media.ScreenMirrorControlsFacts
import kotlin.system.exitProcess

@Keep
class ShizukuMirrorInput : Binder() {
    private val managerClass = if (android.os.Build.VERSION.SDK_INT >= 34)
        Class.forName("android.hardware.input.InputManagerGlobal")
    else Class.forName("android.hardware.input.InputManager")
    private val manager = managerClass.getMethod("getInstance").invoke(null)
    private val inject = managerClass.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
    private val setDisplay = if (android.os.Build.VERSION.SDK_INT >= 29)
        InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType) else null
    private val touch = MirrorPointers { time, action, contacts ->
        val properties = contacts.map { MotionEvent.PointerProperties().apply {
            id = it.slot
            toolType = MotionEvent.TOOL_TYPE_FINGER
        } }.toTypedArray()
        val coords = contacts.map { MotionEvent.PointerCoords().apply {
            x = it.x
            y = it.y
            pressure = it.pressure
            size = 1f
        } }.toTypedArray()
        val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, contacts.size, properties, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
        try { send(event) } finally { event.recycle() }
    }

    @Synchronized
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        when (code) {
            IBinder.FIRST_CALL_TRANSACTION -> {
                val inputs = JsonHelper.jsonDecode<ScreenMirrorControlsFacts>(requireNotNull(data.readString())).inputs
                val width = data.readInt()
                val height = data.readInt()
                for (input in inputs) dispatch(input, width, height)
            }
            IBinder.FIRST_CALL_TRANSACTION + 1 -> touch.reset()
            16777115 -> try { touch.reset() } finally { exitProcess(0) }
            else -> return super.onTransact(code, data, reply, flags)
        }
        reply?.writeNoException()
        return true
    }

    private fun send(event: InputEvent) {
        setDisplay?.invoke(event, android.view.Display.DEFAULT_DISPLAY)
        check(inject.invoke(manager, event, 0) == true) { "Android rejected the input event" }
    }

    private fun dispatch(input: ScreenMirrorControlInput, width: Int, height: Int) {
        fun px(value: Float?, dimension: Int) = value?.let { (it * dimension).coerceIn(0f, (dimension - 1).toFloat()) }
        val x = px(input.x, width)
        val y = px(input.y, height)
        val id = input.pointerId ?: 0
        val now = SystemClock.uptimeMillis()
        when (input.action) {
            TOUCH_DOWN -> touch.down(id, requireNotNull(x), requireNotNull(y), input.pressure ?: 1f, now)
            TOUCH_MOVE -> touch.move(id, requireNotNull(x), requireNotNull(y), input.pressure ?: 1f)
            TOUCH_UP -> touch.up(id, x, y)
            BACK, HOME, RECENTS, LOCK_SCREEN, KEY -> {
                val key = when (input.action) {
                    BACK -> KeyEvent.KEYCODE_BACK
                    HOME -> KeyEvent.KEYCODE_HOME
                    RECENTS -> KeyEvent.KEYCODE_APP_SWITCH
                    LOCK_SCREEN -> KeyEvent.KEYCODE_SLEEP
                    else -> KeyEvent.keyCodeFromString(input.key ?: return)
                }
                if (key == KeyEvent.KEYCODE_UNKNOWN) return
                for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP))
                    send(KeyEvent(now, SystemClock.uptimeMillis(), action, key, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD))
            }
            SCROLL -> {
                val coord = MotionEvent.PointerCoords().apply {
                    this.x = requireNotNull(x)
                    this.y = requireNotNull(y)
                    setAxisValue(MotionEvent.AXIS_HSCROLL, -(input.deltaX ?: 0f))
                    setAxisValue(MotionEvent.AXIS_VSCROLL, -(input.deltaY ?: 0f))
                }
                val mouse = MotionEvent.PointerProperties().apply { this.id = 0; toolType = MotionEvent.TOOL_TYPE_MOUSE }
                val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL, 1, arrayOf(mouse), arrayOf(coord),
                    0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0)
                try { send(event) } finally { event.recycle() }
            }
            TAP, LONG_PRESS, SWIPE, TOUCH -> {
                val points = input.pathPoints.orEmpty().sortedBy { it.tMs }
                val startX = requireNotNull(x ?: px(points.firstOrNull()?.x, width))
                val startY = requireNotNull(y ?: px(points.firstOrNull()?.y, height))
                touch.down(id, startX, startY, 1f, now)
                try {
                    when (input.action) {
                        TAP -> Unit
                        LONG_PRESS -> SystemClock.sleep((input.durationMs ?: 500L).coerceIn(500, 60_000))
                        SWIPE -> {
                            val endX = requireNotNull(px(input.endX, width))
                            val endY = requireNotNull(px(input.endY, height))
                            val duration = (input.durationMs ?: 300L).coerceIn(50, 60_000)
                            do {
                                val elapsed = (SystemClock.uptimeMillis() - now).coerceAtMost(duration)
                                val t = elapsed.toFloat() / duration
                                touch.move(id, startX + (endX - startX) * t, startY + (endY - startY) * t, 1f)
                                if (elapsed == duration) break
                                SystemClock.sleep(minOf(16, duration - elapsed))
                            } while (true)
                        }
                        TOUCH -> {
                            for (point in points) {
                                val offset = if (points.size == 1) point.tMs.toLong() else point.tMs.toLong() - points.first().tMs
                                SystemClock.sleep((now + offset.coerceIn(0, 60_000) - SystemClock.uptimeMillis()).coerceAtLeast(0))
                                touch.move(id, px(point.x, width)!!, px(point.y, height)!!, 1f)
                            }
                        }
                        else -> Unit
                    }
                } finally { touch.up(id, null, null) }
            }
        }
    }
}
