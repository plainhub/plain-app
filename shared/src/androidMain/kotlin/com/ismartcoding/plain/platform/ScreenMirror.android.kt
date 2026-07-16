package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.ScreenMirrorControlInput
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.services.PlainAccessibilityService
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.web.models.ScreenMirrorVideoCodec

actual fun isScreenMirrorRunning(): Boolean =
    ScreenMirrorService.instance?.isRunning() == true

actual fun getScreenMirrorVideoCodec(): ScreenMirrorVideoCodec? =
    ScreenMirrorService.instance?.let { svc ->
        svc.getPipeline()?.getScreenMirrorVideoCodec()
    }

actual fun isScreenMirrorControlEnabled(): Boolean =
    PlainAccessibilityService.isEnabled()

actual fun onScreenMirrorQualityChanged(mode: ScreenMirrorMode) {
    ScreenMirrorService.instance?.onQualityChanged()
}

actual fun stopScreenMirror() {
    ScreenMirrorService.instance?.stop()
    ScreenMirrorService.instance = null
}

actual fun applyScreenMirrorQualityPreference() {
    // The service reads ScreenMirrorService.qualityData lazily; this is a no-op
    // hook kept for symmetry with Android's pre-start quality seeding.
}

actual fun dispatchScreenMirrorControl(input: ScreenMirrorControlInput): Boolean {
    val service = PlainAccessibilityService.instance ?: return false
    val screenSize = PlainAccessibilityService.getScreenSize(appContext)
    service.dispatchControl(input, screenSize.x, screenSize.y)
    return true
}

actual fun getAccessibilityScreenSize(): Pair<Int, Int> {
    val size = PlainAccessibilityService.getScreenSize(appContext)
    return Pair(size.x, size.y)
}
