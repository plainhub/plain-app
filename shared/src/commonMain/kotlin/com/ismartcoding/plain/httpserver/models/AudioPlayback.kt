package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class AudioPlayback(
    val mode: MediaPlayMode = MediaPlayMode.REPEAT,
    val currentPath: String = "",
)
