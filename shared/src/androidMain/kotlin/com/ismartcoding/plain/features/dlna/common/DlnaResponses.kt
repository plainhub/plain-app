package com.ismartcoding.plain.features.dlna.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DlnaTransportInfoResponse {
    @SerialName("CurrentTransportState") val state: String = ""
    @SerialName("CurrentTransportStatus") val status: String = ""
    @SerialName("CurrentSpeed") val speed: Int = 0
}

@Serializable
class DlnaPositionInfoResponse {
    @SerialName("Track") val track: String = ""
    @SerialName("TrackDuration") val trackDuration: String = ""
    @SerialName("TrackMetaData") val trackMetaData: String = ""
    @SerialName("TrackURI") val trackURI: String = ""
    @SerialName("RelTime") val relTime: String = ""
    @SerialName("AbsTime") val absTime: String = ""
    @SerialName("RelCount") val relCount: String = ""
    @SerialName("AbsCount") val absCount: String = ""
}

@Serializable
class DlnaSetUriResponse
