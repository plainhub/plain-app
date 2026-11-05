package com.ismartcoding.plain.lib

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generateId(): String {
    return Uuid.random().toString()
}
