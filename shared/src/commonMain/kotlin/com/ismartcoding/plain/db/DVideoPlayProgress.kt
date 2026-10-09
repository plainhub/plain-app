package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

data class DVideoPlayProgress(
    val mediaId: String,
    val positionMs: Long,
    val updatedAt: Instant = TimeHelper.now(),
)
