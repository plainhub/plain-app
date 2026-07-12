package com.ismartcoding.plain.web.models

import kotlin.time.Instant

data class Call(
    var id: ID,
    var number: String,
    var name: String,
    var photoId: String,
    var startedAt: Instant,
    var duration: Int,
    var type: Int,
    val accountId: ID,
    val geo: PhoneGeo?,
)
