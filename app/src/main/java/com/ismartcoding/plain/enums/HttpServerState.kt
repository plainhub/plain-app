package com.ismartcoding.plain.enums

import android.os.Parcelable
import com.ismartcoding.plain.R
import kotlinx.parcelize.Parcelize

@Parcelize
enum class HttpServerState: Parcelable {
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