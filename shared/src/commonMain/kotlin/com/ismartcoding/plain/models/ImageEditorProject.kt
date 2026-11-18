package com.ismartcoding.plain.models

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Required

@Serializable
data class ImageEditorProject(
    var id: String,
    @Required var stateB64: String = "",
    @Required var thumbnail: String? = null,
    @Required var canvasWidth: Int = 0,
    @Required var canvasHeight: Int = 0,
    @Required var createdAt: Instant = TimeHelper.now(),
    @Required var updatedAt: Instant = TimeHelper.now(),
)
