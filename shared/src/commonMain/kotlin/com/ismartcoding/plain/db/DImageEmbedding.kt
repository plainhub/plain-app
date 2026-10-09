package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import com.ismartcoding.plain.db.IData

data class DImageEmbedding(
    override var id: String,

    val path: String,

    val embedding: ByteArray,

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) : IData
