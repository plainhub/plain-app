package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.ScreenMirrorControlInput
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.web.models.ScreenMirrorVideoCodec

actual fun isScreenMirrorRunning(): Boolean = false

actual fun getScreenMirrorVideoCodec(): ScreenMirrorVideoCodec? = null

actual fun isScreenMirrorControlEnabled(): Boolean = false

actual fun onScreenMirrorQualityChanged(mode: ScreenMirrorMode) = Unit

actual fun stopScreenMirror() = Unit

actual fun applyScreenMirrorQualityPreference() = Unit

actual fun dispatchScreenMirrorControl(input: ScreenMirrorControlInput): Boolean = false

actual fun getAccessibilityScreenSize(): Pair<Int, Int> = Pair(0, 0)
