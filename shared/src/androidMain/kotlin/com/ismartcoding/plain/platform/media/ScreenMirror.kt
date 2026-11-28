package com.ismartcoding.plain.platform.media


import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.system.ShizukuHelper
import com.ismartcoding.plain.services.PlainAccessibilityService
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.models.ScreenMirrorVideoCodec
import com.ismartcoding.plain.services.StreamTouchInjector
import com.ismartcoding.plain.services.TouchInjectThread
import kotlinx.serialization.json.JsonObject

fun isScreenMirrorRunning(): Boolean =
    ScreenMirrorService.instance?.isRunning() == true

suspend fun getScreenMirrorVideoCodec(): ScreenMirrorVideoCodec? =
    ScreenMirrorService.instance?.let { svc ->
        svc.getPipeline()?.awaitVideoCodec()
    }

fun isScreenMirrorControlEnabled(): Boolean =
    ShizukuHelper.isGranted() || PlainAccessibilityService.isEnabled()

fun stopScreenMirror() {
    ScreenMirrorService.instance?.stop()
    ScreenMirrorService.instance = null
}

suspend fun dispatchScreenMirrorControls(params: JsonObject) {
    val mirror = ScreenMirrorService.instance?.takeIf { it.isRunning() } ?: return
    val size = PlainAccessibilityService.getScreenSize(appContext)
    if (ShizukuHelper.isGranted()) {
        mirror.shizukuInput.dispatch(params.toString(), size)
    } else {
        val service = PlainAccessibilityService.instance ?: return
        val inputs = JsonHelper.jsonDecodeFromElement<ScreenMirrorControlsFacts>(params).inputs
        for (input in inputs) service.dispatchControl(input, size.x, size.y)
    }
}

fun requestScreenMirrorKeyFrame() {
    ScreenMirrorService.instance?.getPipeline()?.requestKeyFrame()
}


fun resetScreenMirrorTouchStream() {
    ScreenMirrorService.instance?.shizukuInput?.reset()
    TouchInjectThread.handler.post {
        StreamTouchInjector.reset()
    }
}
