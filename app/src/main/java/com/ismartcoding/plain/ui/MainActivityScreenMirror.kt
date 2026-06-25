package com.ismartcoding.plain.ui

import com.ismartcoding.plain.i18n.*

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper

internal fun MainActivity.sendScreenMirrorAudioStatus(granted: Boolean) {
    sendEvent(WebSocketEvent(EventType.SCREEN_MIRROR_AUDIO_GRANTED, JsonHelper.jsonEncode(granted)))
}

internal fun MainActivity.openAppDetailsSettingsForAudio() {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        appDetailsSettingsForAudioLauncher.launch(intent)
    } catch (e: Exception) {
        try {
            appDetailsSettingsForAudioLauncher.launch(Intent(Settings.ACTION_SETTINGS))
        } catch (e2: Exception) {
            DialogHelper.showMessage(LocaleHelper.getString(Res.string.open_permission_settings))
            sendScreenMirrorAudioStatus(false)
        }
    }
}

internal fun MainActivity.showRecordAudioPermissionSettingsGuide() {
    DialogHelper.showConfirmDialog(
        LocaleHelper.getString(Res.string.permission_required),
        LocaleHelper.getString(Res.string.screen_mirror_audio_permission_settings_message),
        confirmButton = Pair(LocaleHelper.getString(Res.string.view_in_settings)) { openAppDetailsSettingsForAudio() },
        dismissButton = Pair(LocaleHelper.getString(Res.string.cancel)) { sendScreenMirrorAudioStatus(false) },
    )
}
