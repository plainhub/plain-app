package com.ismartcoding.plain.enums

enum class HttpServerState {
    OFF,
    ON,
    STARTING,
    STOPPING,
    ERROR;

    fun isProcessing(): Boolean {
        return setOf(STARTING, STOPPING).contains(this)
    }
}
