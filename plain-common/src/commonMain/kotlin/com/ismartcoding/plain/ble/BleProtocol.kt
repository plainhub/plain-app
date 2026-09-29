package com.ismartcoding.plain.ble

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val bleJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }


@Serializable
data class BleRequestData(
    @SerialName("h") val headers: Map<String, String> = emptyMap(),
    @SerialName("b") val body: String = "",
) {
    companion object {
        fun create(headers: Map<String, String> = emptyMap()): BleRequestData = BleRequestData(headers = headers)

        fun fromJSON(json: String): BleRequestData = bleJson.decodeFromString(json)
    }

    fun toJSON(): String = bleJson.encodeToString(this)
}

@Serializable
data class BleSegmentData(
    @SerialName("d") val data: String,
    @SerialName("s") val state: Int,
) {
    fun isEnd(): Boolean = (state and STATE_END_BIT) == STATE_END_BIT

    fun toJSON(): String = bleJson.encodeToString(this)

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

        fun fromJSON(value: String): BleSegmentData = bleJson.decodeFromString(value)
    }
}
