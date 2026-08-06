package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.enums.MediaPlayMode

@GraphQLType
data class App(
    val clientId: String,
    val usbConnected: Boolean,
    val urlToken: String,
    val httpPort: Int,
    val httpsPort: Int,
    val appDir: String,
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
    val debug: Boolean,
)
