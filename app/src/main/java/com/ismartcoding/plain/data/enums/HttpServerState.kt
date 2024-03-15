package com.ismartcoding.plain.data.enums

import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString

enum class HttpServerState {
    OFF,
    ON,
    STARTING,
    STOPPING,
    ERROR;

    fun getTextId(): Int {
        return when(this) {
            OFF -> R.string.start_service
            ON -> R.string.http_server_state_on
            STARTING -> R.string.http_server_state_starting
            STOPPING -> R.string.http_server_state_stopping
            ERROR -> R.string.http_server_state_error
        }
    }

    fun isProcessing(): Boolean {
        return setOf(STARTING, STOPPING).contains(this)
    }
}