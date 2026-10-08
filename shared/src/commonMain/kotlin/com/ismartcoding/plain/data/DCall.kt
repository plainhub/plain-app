package com.ismartcoding.plain.data

import com.ismartcoding.plain.db.IData
import kotlin.time.Instant

data class DCall(
    override var id: String,
    var number: String,
    var name: String,
    var photoUri: String,
    var startedAt: Instant,
    var durationSec: Int,
    var type: Int,
    val accountId: String,
) : IData
