package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.data.ScreenMirrorControlInput
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.events.HRequestScreenMirrorAudioEvent
import com.ismartcoding.plain.events.HStartScreenMirrorEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.applyScreenMirrorQualityPreference
import com.ismartcoding.plain.platform.dispatchScreenMirrorControl
import com.ismartcoding.plain.platform.getScreenMirrorVideoCodec
import com.ismartcoding.plain.platform.isScreenMirrorControlEnabled
import com.ismartcoding.plain.platform.isScreenMirrorRunning
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.onScreenMirrorQualityChanged
import com.ismartcoding.plain.platform.stopScreenMirror
import com.ismartcoding.plain.preferences.ScreenMirrorQualityPreference
import com.ismartcoding.plain.web.models.ScreenMirrorVideoCodec
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addScreenMirrorSchema() {
    query("screenMirrorState") {
        resolver { ->
            isScreenMirrorRunning()
        }
    }
    query("screenMirrorVideoCodec") {
        resolver { ->
            getScreenMirrorVideoCodec()
        }
    }
    query("screenMirrorControlEnabled") {
        resolver { ->
            isScreenMirrorControlEnabled()
        }
    }
    query("screenMirrorQuality") {
        resolver { ->
            ScreenMirrorQualityPreference.getValueAsync().toModel()
        }
    }
    mutation("startScreenMirror") {
        resolver("audio") { audio: Boolean ->
            applyScreenMirrorQualityPreference()
            sendEvent(HStartScreenMirrorEvent(audio))
            true
        }
    }
    mutation("requestScreenMirrorAudio") {
        resolver { ->
            if (Permission.RECORD_AUDIO.isGranted()) {
                true
            } else {
                sendEvent(HRequestScreenMirrorAudioEvent())
                false
            }
        }
    }
    mutation("stopScreenMirror") {
        resolver { ->
            stopScreenMirror()
            true
        }
    }
    mutation("updateScreenMirrorQuality") {
        resolver("mode") { mode: ScreenMirrorMode ->
            val resolution = when (mode) {
                ScreenMirrorMode.SMOOTH -> 720
                ScreenMirrorMode.HD -> 1080
            }
            val qualityData = DScreenMirrorQuality(mode, resolution)
            ScreenMirrorQualityPreference.putAsync(qualityData)
            onScreenMirrorQualityChanged(mode)
            true
        }
    }
    mutation("sendScreenMirrorControl") {
        resolver("input") { input: ScreenMirrorControlInput ->
            val ok = dispatchScreenMirrorControl(input)
            if (!ok) {
                throw GraphQLError("Accessibility service is not enabled")
            }
            true
        }
    }
    type<ScreenMirrorVideoCodec> {}
}
