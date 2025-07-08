package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.enums.MediaPlayMode

data class App(
    val usbConnected: Boolean,
    val urlToken: String,
    val httpPort: Int,
    val httpsPort: Int,
    val externalFilesDir: String,
    val deviceName: String,
    val battery: Int,
    val appVersion: Int,
    val osVersion: Int,
    val channel: String,
    val permissions: List<Permission>,
    val audios: List<PlaylistAudio>,
    val audioMode: MediaPlayMode,
    val audioCurrent: String,
    val sdcardPath: String,
    val usbDiskPaths: List<String>,
    val internalStoragePath: String,
    val downloadsDir: String,
    val developerMode: Boolean,
    val favoriteFolders: List<FavoriteFolder>,
    val customChatFolder: String,
)
