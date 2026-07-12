package com.ismartcoding.plain.web

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

val onlineClientIds = MutableStateFlow<Set<String>>(emptySet())

private val _wsSessionCount = MutableStateFlow(0)
val wsSessionCount: StateFlow<Int> = _wsSessionCount

internal fun setOnlineClientIds(ids: Set<String>) {
    onlineClientIds.value = ids
    _wsSessionCount.value = ids.size
}
