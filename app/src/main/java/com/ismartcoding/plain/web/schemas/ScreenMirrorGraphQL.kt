package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.data.ScreenMirrorControlInput
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.events.HRequestScreenMirrorAudioEvent
import com.ismartcoding.plain.events.HStartScreenMirrorEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.preferences.ScreenMirrorQualityPreference
import com.ismartcoding.plain.services.PlainAccessibilityService
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.models.ScreenMirrorVideoCodec

fun SchemaBuilder.addScreenMirrorSchema() {
    query("screenMirrorState") {
        resolver { ->
            ScreenMirrorService.instance?.isRunning() == true
        }
    }
    query("screenMirrorVideoCodec") {
        resolver { ->
            ScreenMirrorService.instance?.let { svc ->
                svc.getPipeline()?.getScreenMirrorVideoCodec()
            }
        }
    }
    query("screenMirrorControlEnabled") {
        resolver { ->
            PlainAccessibilityService.isEnabled()
        }
    }
    query("screenMirrorQuality") {
        resolver { ->
            ScreenMirrorQualityPreference.getValueAsync().toModel()
        }
    }
    mutation("startScreenMirror") {
        resolver { audio: Boolean ->
            ScreenMirrorService.qualityData = ScreenMirrorQualityPreference.getValueAsync()
            sendEvent(HStartScreenMirrorEvent(audio))
            true
        }
    }
    mutation("requestScreenMirrorAudio") {
        resolver { ->
            if (Permission.RECORD_AUDIO.can(MainApp.instance)) {
                true
            } else {
                sendEvent(HRequestScreenMirrorAudioEvent())
                false
            }
        }
    }
    mutation("stopScreenMirror") {
        resolver { ->
            ScreenMirrorService.instance?.stop()
            ScreenMirrorService.instance = null
            true
        }
    }
    mutation("updateScreenMirrorQuality") {
        resolver { mode: ScreenMirrorMode ->
            val resolution = when (mode) {
                ScreenMirrorMode.SMOOTH -> 720
                ScreenMirrorMode.HD -> 1080
            }
            val qualityData = DScreenMirrorQuality(mode, resolution)
            ScreenMirrorQualityPreference.putAsync(qualityData)
            ScreenMirrorService.qualityData = qualityData
            ScreenMirrorService.instance?.onQualityChanged()
            true
        }
    }
    mutation("sendScreenMirrorControl") {
        resolver { input: ScreenMirrorControlInput ->
            val service = PlainAccessibilityService.instance
                ?: throw GraphQLError("Accessibility service is not enabled")
            val screenSize = PlainAccessibilityService.getScreenSize(MainApp.instance)
            service.dispatchControl(input, screenSize.x, screenSize.y)
            true
        }
    }
    type<ScreenMirrorVideoCodec> {}
}
