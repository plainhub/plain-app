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

class GetPositionInfoResponse {
    @SerializedName("Track")
    val track: String = ""

    @SerializedName("TrackDuration")
    val trackDuration: String = ""

    @SerializedName("TrackMetaData")
    val trackMetaData: String = ""

    @SerializedName("TrackURI")
    val trackURI: String = ""

    @SerializedName("RelTime")
    val relTime: String = ""

    @SerializedName("AbsTime")
    val absTime: String = ""

    @SerializedName("RelCount")
    val relCount: String = ""

    @SerializedName("AbsCount")
    val absCount: String = ""
}

class SetAVTransportURIResponse
