package com.ismartcoding.lib.upnp

import com.google.gson.annotations.SerializedName

class GetTransportInfoResponse {
    @SerializedName("CurrentTransportState")
    val state: String = ""

    @SerializedName("CurrentTransportStatus")
    val status: String = ""

    @SerializedName("CurrentSpeed")
    val speed: Int = 0
}

class SetAVTransportURIResponse
