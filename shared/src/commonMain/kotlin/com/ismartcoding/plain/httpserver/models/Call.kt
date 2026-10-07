package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.enums.CallType
import kotlin.time.Instant

data class Call(
    var id: ID,
    var number: String,
    var name: String,
    var photoId: String,
    var startedAt: Instant,
    var durationSec: Int,
    var type: CallType,
    val accountId: ID,
    val geo: PhoneGeo?,
)
