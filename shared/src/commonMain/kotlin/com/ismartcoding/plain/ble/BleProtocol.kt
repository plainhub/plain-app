package com.ismartcoding.plain.ble

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.platform.getAppVersion
import com.ismartcoding.plain.platform.getDeviceName
import com.ismartcoding.plain.platform.getPlatformName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BleAuthData(val password: String)

@Serializable
data class BleRequestData(
    @SerialName("h") val headers: Map<String, String> = emptyMap(),
    @SerialName("b") val body: String = "",
) {
    companion object {
        fun create(): BleRequestData {
            return BleRequestData(
                headers = mapOf(
                    "c-id" to TempData.clientId,
                    "c-platform" to getPlatformName(),
                    "c-name" to getDeviceName(),
                    "c-version" to getAppVersion(),
                ),
            )
        }

        fun fromJSON(json: String): BleRequestData = JsonHelper.jsonDecode(json)
    }

    fun toJSON(): String = JsonHelper.jsonEncode(this)
}

@Serializable
data class BleSegmentData(
    @SerialName("d") val data: String,
    @SerialName("s") val state: Int,
) {
    fun isEnd(): Boolean = (state and STATE_END_BIT) == STATE_END_BIT

    fun toJSON(): String = JsonHelper.jsonEncode(this)

    companion object {
        private const val STATE_START_BIT = 1
        const val STATE_END_BIT = 2

        fun build(
            data: String,
            start: Boolean,
            end: Boolean,
        ): BleSegmentData {
            var state = 0
            if (start && end) {
                state = STATE_START_BIT or STATE_END_BIT
            } else if (start) {
                state = STATE_START_BIT
            } else if (end) {
                state = STATE_END_BIT
            }
            return BleSegmentData(data, state)
        }

        fun fromJSON(value: String): BleSegmentData = JsonHelper.jsonDecode(value)
    }
}
