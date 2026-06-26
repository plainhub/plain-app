package com.ismartcoding.plain.data

import kotlin.time.Instant

data class DCall(
    override var id: String,
    var number: String,
    var name: String,
    var photoUri: String,
    var startedAt: Instant,
    var duration: Int,
    var type: Int,
    val accountId: String,
) : IData
