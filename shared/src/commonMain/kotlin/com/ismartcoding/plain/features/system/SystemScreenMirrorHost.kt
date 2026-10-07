package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.isGranted
import kotlinx.serialization.json.*

internal object SystemScreenMirrorHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemScreenMirrorState" -> {
            val codec = com.ismartcoding.plain.platform.getScreenMirrorVideoCodec()
            JsonHelper.jsonEncodeToElement(ScreenMirrorStateFacts(
                running = com.ismartcoding.plain.platform.isScreenMirrorRunning(),
                controlEnabled = com.ismartcoding.plain.platform.isScreenMirrorControlEnabled(),
                codec = codec?.let { value ->
                    ScreenMirrorCodecFacts(
                        annexB = value.annexB,
                        keyFrame = value.keyFrame,
                    )
                },
            ))
        }
        "systemScreenMirrorQuality" -> JsonHelper.jsonEncodeToElement(UserPrefs.screenMirrorQualityValue().toModel())
        "systemStartScreenMirror" -> {
            com.ismartcoding.plain.platform.applyScreenMirrorQualityPreference()
            sendEvent(
                com.ismartcoding.plain.events.HStartScreenMirrorEvent(
                    params.getValue("audio").jsonPrimitive.boolean
                )
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemStopScreenMirror" -> {
            com.ismartcoding.plain.platform.stopScreenMirror()
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemRequestScreenMirrorAudio" -> {
            val granted = com.ismartcoding.plain.platform.Permission.RECORD_AUDIO.isGranted()
            if (!granted) {
                sendEvent(
                    com.ismartcoding.plain.events.HRequestScreenMirrorAudioEvent()
                )
            }
            JsonHelper.jsonEncodeToElement(granted)
        }
        "systemRequestScreenMirrorKeyFrame" -> {
            com.ismartcoding.plain.platform.requestScreenMirrorKeyFrame()
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemUpdateScreenMirrorQuality" -> {
            val mode = when (params.getValue("mode").jsonPrimitive.content) {
                "SMOOTH" -> com.ismartcoding.plain.enums.ScreenMirrorMode.SMOOTH
                else -> com.ismartcoding.plain.enums.ScreenMirrorMode.HD
            }
            val quality = DScreenMirrorQuality(
                mode, if (mode == com.ismartcoding.plain.enums.ScreenMirrorMode.SMOOTH) 720 else 1080
            )
            com.ismartcoding.plain.preferences.UserPrefs.setScreenMirrorQuality(quality)
            com.ismartcoding.plain.platform.onScreenMirrorQualityChanged(mode)
            JsonHelper.jsonEncodeToElement(true)
        }
        else -> error("Unsupported provider operation")
    }
}
