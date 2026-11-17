package com.ismartcoding.plain.features.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ServiceState {
    @SerialName("STOPPED")
    OFF,
    @SerialName("STARTED")
    ON,
    @SerialName("STARTING")
    STARTING,
    @SerialName("STOPPING")
    STOPPING,
    @SerialName("ERROR")
    ERROR;

    fun isProcessing(): Boolean {
        return setOf(STARTING, STOPPING).contains(this)
    }
}
