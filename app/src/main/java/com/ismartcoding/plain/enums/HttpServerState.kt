package com.ismartcoding.plain.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class HttpServerState: Parcelable {
    OFF,
    ON,
    STARTING,
    STOPPING,
    ERROR;

    fun isProcessing(): Boolean {
        return setOf(STARTING, STOPPING).contains(this)
    }
}