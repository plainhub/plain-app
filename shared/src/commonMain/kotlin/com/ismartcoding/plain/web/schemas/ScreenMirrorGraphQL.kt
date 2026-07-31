package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
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
import com.ismartcoding.plain.platform.requestScreenMirrorKeyFrame
import com.ismartcoding.plain.platform.stopScreenMirror
import com.ismartcoding.plain.preferences.ScreenMirrorQualityPreference
import com.ismartcoding.plain.web.models.ScreenMirrorQuality
import com.ismartcoding.plain.web.models.ScreenMirrorVideoCodec
import com.ismartcoding.plain.web.models.toModel

@GraphQLQuery
suspend fun screenMirrorState(): Boolean {
    return isScreenMirrorRunning()
}

@GraphQLQuery
suspend fun screenMirrorVideoCodec(): ScreenMirrorVideoCodec? {
    return getScreenMirrorVideoCodec()
}

@GraphQLQuery
suspend fun screenMirrorControlEnabled(): Boolean {
    return isScreenMirrorControlEnabled()
}

@GraphQLQuery
suspend fun screenMirrorQuality(): ScreenMirrorQuality {
    return ScreenMirrorQualityPreference.getValueAsync().toModel()
}

@GraphQLMutation
suspend fun startScreenMirror(audio: Boolean): Boolean {
    applyScreenMirrorQualityPreference()
    sendEvent(HStartScreenMirrorEvent(audio))
    return true
}

@GraphQLMutation
suspend fun requestScreenMirrorAudio(): Boolean {
    if (Permission.RECORD_AUDIO.isGranted()) {
        return true
    } else {
        sendEvent(HRequestScreenMirrorAudioEvent())
        return false
    }
}

@GraphQLMutation
suspend fun stopScreenMirror(): Boolean {
    com.ismartcoding.plain.platform.stopScreenMirror()
    return true
}

@GraphQLMutation
suspend fun updateScreenMirrorQuality(mode: ScreenMirrorMode): Boolean {
    val resolution = when (mode) {
        ScreenMirrorMode.SMOOTH -> 720
        ScreenMirrorMode.HD -> 1080
    }
    val qualityData = DScreenMirrorQuality(mode, resolution)
    ScreenMirrorQualityPreference.putAsync(qualityData)
    onScreenMirrorQualityChanged(mode)
    return true
}

@GraphQLMutation
suspend fun sendScreenMirrorControl(input: ScreenMirrorControlInput): Boolean {
    val ok = dispatchScreenMirrorControl(input)
    if (!ok) {
        throw GraphQLError("Accessibility service is not enabled")
    }
    return true
}

@GraphQLMutation
suspend fun requestScreenMirrorKeyFrame(): Boolean {
    com.ismartcoding.plain.platform.requestScreenMirrorKeyFrame()
    return true
}

fun SchemaBuilder.addScreenMirrorSchema() {
    type<ScreenMirrorVideoCodec> {}
}
