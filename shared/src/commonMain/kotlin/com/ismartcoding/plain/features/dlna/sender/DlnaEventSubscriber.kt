package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.dlna.common.DlnaDevice
import com.ismartcoding.plain.platform.PlainRequest
import com.ismartcoding.plain.platform.createHttpClient
import com.ismartcoding.plain.platform.request

object DlnaEventSubscriber {

    suspend fun subscribeEvent(device: DlnaDevice, callbackUrl: String): String {
        val service = device.getAVTransportService() ?: return ""
        return try {
            val response = withIO {
                createHttpClient().request(
                    PlainRequest(
                        "SUBSCRIBE",
                        device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/'),
                        headers = mapOf(
                            "NT" to "upnp:event",
                            "TIMEOUT" to "Second-3600",
                            "CALLBACK" to "<$callbackUrl>",
                        ),
                    ),
                )
            }
            response.use { it.header("SID").orEmpty() }
        } catch (ex: Exception) { ex.printStackTrace(); "" }
    }

    suspend fun renewEvent(device: DlnaDevice, sid: String): String {
        val service = device.getAVTransportService() ?: return ""
        return try {
            val response = withIO {
                createHttpClient().request(
                    PlainRequest(
                        "SUBSCRIBE",
                        device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/'),
                        headers = mapOf(
                            "SID" to sid,
                            "TIMEOUT" to "Second-3600",
                        ),
                    ),
                )
            }
            response.use { it.header("SID").orEmpty() }
        } catch (ex: Exception) { ex.printStackTrace(); "" }
    }

    suspend fun unsubscribeEvent(device: DlnaDevice, sid: String): String {
        val service = device.getAVTransportService() ?: return ""
        return try {
            val response = withIO {
                createHttpClient().request(
                    PlainRequest(
                        "UNSUBSCRIBE",
                        device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/'),
                        headers = mapOf("SID" to sid),
                    ),
                )
            }
            response.use {
                LogCat.e(it.toString())
                val xml = it.bodyAsText()
                LogCat.e(xml)
                if (it.isOk()) xml else ""
            }
        } catch (ex: Exception) { ex.printStackTrace(); "" }
    }
}
