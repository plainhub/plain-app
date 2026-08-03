package com.ismartcoding.plain.web

import kotlinx.coroutines.flow.MutableStateFlow

val onlineClientIds = MutableStateFlow<Set<String>>(emptySet())

internal fun setOnlineClientIds(ids: Set<String>) {
    onlineClientIds.value = ids
}
