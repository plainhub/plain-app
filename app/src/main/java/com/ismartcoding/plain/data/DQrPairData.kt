package com.ismartcoding.plain.data

import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.serialization.Serializable

@Serializable
data class DQrPairData(
    val id: String,
    val name: String,
    val port: Int,
    val deviceType: DeviceType,
    val ips: List<String> = emptyList(), // All IP addresses
) {
    fun toQrContent(): String = QR_PREFIX + JsonHelper.jsonEncode(this)

    fun toDNearbyDevice(): DNearbyDevice {
        return DNearbyDevice(
            id = id,
            name = name,
            ips = ips,
            port = port,
            deviceType = deviceType,
            version = "",
            platform = "android",
            lastSeen = TimeHelper.now(),
        )
    }

    companion object {
        const val QR_PREFIX = "plainapp://pair/"

        fun fromQrContent(content: String): DQrPairData? {
            return try {
                if (!content.startsWith(QR_PREFIX)) return null
                val json = content.removePrefix(QR_PREFIX)
                JsonHelper.jsonDecode<DQrPairData>(json)
            } catch (e: Exception) {
                LogCat.e("Failed to parse pair QR data: ${e.message}")
                null
            }
        }
    }
}
