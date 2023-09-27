package com.ismartcoding.plain.features.hostapd

import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import okio.utf8Size

data class HostapdConfig(
    var ssid: String = "",
    var password: String = "",
    var hideSsid: Boolean = false,
    var config: String = "",
) {
    fun parse(): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        config.split("\n").forEach { line ->
            val split = line.split("=")
            if (split.size > 1) {
                map[split[0].trimEnd()] = split[1].trimStart()
            }
        }
        return map
    }

    fun load(config: String) {
        this.config = config
        val map = parse()
        ssid = map["ssid"] ?: ""
        password = map["wpa_passphrase"] ?: ""
        hideSsid = map["ignore_broadcast_ssid"] == "1"
    }

    fun toConfig(): String {
        val map = parse()
        map["ssid"] = ssid
        map["wpa_passphrase"] = password
        map["ignore_broadcast_ssid"] = if (hideSsid) "1" else "0"
        val items = mutableListOf<String>()
        map.forEach {
            items.add("${it.key}=${it.value}")
        }

        return items.joinToString("\n")
    }

    fun validate(): String {
        if (ssid.length < 2 || ssid.utf8Size() > 32) {
            return getString(R.string.hostapd_ssid_length_error)
        }

        if (password.length < 8 || password.utf8Size() > 63) {
            return getString(R.string.hostapd_wpa_passharase_length_error)
        }

        return ""
    }

    fun validateSSID(value: String): String {
        if (value.length < 2 || value.utf8Size() > 32) {
            return getString(R.string.ssid_length_error)
        }
        return ""
    }

    fun validatePassword(value: String): String {
        if (value.length < 8 || value.utf8Size() > 63) {
            return getString(R.string.wifi_password_length_error)
        }
        return ""
    }
}
