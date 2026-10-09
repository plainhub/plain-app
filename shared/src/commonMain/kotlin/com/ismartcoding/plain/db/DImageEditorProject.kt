package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

data class DImageEditorProject(
    val id: String,

    var stateB64: String = "",

    var thumbnail: String? = null,

    var canvasWidth: Int = 0,

    var canvasHeight: Int = 0,

    var layerCount: Int = 0,

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
)
