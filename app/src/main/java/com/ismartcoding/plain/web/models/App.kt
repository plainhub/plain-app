package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.audio.MediaPlayMode
import com.ismartcoding.plain.features.theme.AppTheme

data class App(
    val usbConnected: Boolean,
    val fileIdToken: String,
    val externalFilesDir: String,
    val deviceName: String,
    val battery: Int,
    val locale: String,
    val theme: AppTheme,
    val version: String,
    val permissions: List<Permission>,
    val audios: List<PlaylistAudio>,
    val audioMode: MediaPlayMode,
    val audioCurrent: String,
    val allowSensitivePermissions: Boolean,
)