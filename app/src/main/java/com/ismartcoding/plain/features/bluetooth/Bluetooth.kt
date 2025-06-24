package com.ismartcoding.plain.features.bluetooth

import android.content.Context
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import org.json.JSONObject

data class BleAuthData(val password: String) {
    fun toJSON(): JSONObject {
        val json = JSONObject()
        json.put("password", password)
        return json
    }
}

data class BleRequestData(val headers: MutableMap<String, String> = mutableMapOf(), var body: String = "") {
    fun toJSON(): JSONObject {
        val json = JSONObject()
        val headersJSON = JSONObject()
        headers.forEach {
            headersJSON.put(it.key, it.value)
        }
        json.put("h", headersJSON)
        json.put("b", body)

        return json
    }

    companion object {
        fun create(context: Context): BleRequestData {
            val data = BleRequestData()
            data.headers.run {
                this["c-id"] = TempData.clientId
                this["c-platform"] = "android"
                this["c-name"] = PhoneHelper.getDeviceName(context)
                this["c-version"] = MainApp.getAppVersion()
            }
            return data
        }
    }
}

data class BleSegmentData(val data: String, val state: Int) {
    fun toJSON(): JSONObject {
        val json = JSONObject()

        json.put("s", state)
        json.put("d", data)

        return json
    }

    fun isEnd(): Boolean {
        return (state and stateEndBit) == stateEndBit
    }

    companion object {
        private const val stateStartBit = 1
        const val stateEndBit = 2

        fun build(
            data: String,
            start: Boolean,
            end: Boolean,
        ): BleSegmentData {
            var state = 0
            if (start && end) {
                state = stateStartBit or stateEndBit
            } else if (start) {
                state = stateStartBit
            } else if (end) {
                state = stateEndBit
            }

            return BleSegmentData(data, state)
        }

        fun fromJSON(value: String): BleSegmentData {
            val json = JSONObject(value)
            return BleSegmentData(json.optString("d"), json.optInt("s"))
        }
    }
}
