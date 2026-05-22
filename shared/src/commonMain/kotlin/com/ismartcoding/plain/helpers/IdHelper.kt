package com.ismartcoding.plain.helpers

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generateId(): String {
    val bytes = Uuid.random().toString().encodeToByteArray()
    var value = 0L
    for (i in 0 until 8) {
        value = (value shl 8) or (bytes[i].toLong() and 0xFF)
    }
    return value.toString(36)
}
