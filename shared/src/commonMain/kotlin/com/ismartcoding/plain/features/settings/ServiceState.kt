package com.ismartcoding.plain.features.settings

import kotlinx.serialization.Serializable

@Serializable
enum class ServiceState {
    STOPPED,
    STARTED,
    STARTING,
    STOPPING,
    ERROR;

    fun isProcessing(): Boolean {
        return setOf(STARTING, STOPPING).contains(this)
    }
}
