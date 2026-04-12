package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.dlna.common.DlnaDevice
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

object DlnaEventSubscriber {

    suspend fun subscribeEvent(device: DlnaDevice, callbackUrl: String): String {
        val service = device.getAVTransportService() ?: return ""
        return try {
            val response = withIO {
                HttpClient(CIO).subscribe(device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/')) {
                    headers {
                        set("NT", "upnp:event")
                        set("TIMEOUT", "Second-3600")
                        set("CALLBACK", "<$callbackUrl>")
                    }
                }
            }
            response.headers["SID"].orEmpty()
        } catch (ex: Exception) { ex.printStackTrace(); "" }
    }

    suspend fun renewEvent(device: DlnaDevice, sid: String): String {
        val service = device.getAVTransportService() ?: return ""
        return try {
            val response = withIO {
                HttpClient(CIO).subscribe(device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/')) {
                    headers { set("SID", sid); set("TIMEOUT", "Second-3600") }
                }
            }
            response.headers["SID"].orEmpty()
        } catch (ex: Exception) { ex.printStackTrace(); "" }
    }

    suspend fun unsubscribeEvent(device: DlnaDevice, sid: String): String {
        val service = device.getAVTransportService() ?: return ""
        return try {
            val response = withIO {
                HttpClient(CIO).unsubscribe(device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/')) {
                    headers { set("SID", sid) }
                }
            }
            LogCat.e(response.toString())
            val xml = response.body<String>()
            LogCat.e(xml)
            if (response.status == HttpStatusCode.OK) xml else ""
        } catch (ex: Exception) { ex.printStackTrace(); "" }
    }

    private suspend fun HttpClient.subscribe(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = request(HttpRequestBuilder().apply { method = HttpMethod("SUBSCRIBE"); url(urlString); block() })

    private suspend fun HttpClient.unsubscribe(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = request(HttpRequestBuilder().apply { method = HttpMethod("UNSUBSCRIBE"); url(urlString); block() })
}
