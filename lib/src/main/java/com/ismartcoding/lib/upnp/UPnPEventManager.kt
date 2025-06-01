package com.ismartcoding.lib.upnp

import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
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

/**
 * UPnP Event Manager
 * Handles UPnP device event subscription, renewal and unsubscription
 */
object UPnPEventManager {
    
    /**
     * Subscribe to UPnP device events
     * @param device UPnP device
     * @param callbackUrl Callback URL for event notifications
     * @return Subscription ID (SID)
     */
    suspend fun subscribeEvent(
        device: UPnPDevice,
        callbackUrl: String,
    ): String {
        val service = device.getAVTransportService() ?: return ""
        try {
            val client = HttpClient(CIO)
            val response = withIO {
                client.subscribe(device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/')) {
                    headers {
                        set("NT", "upnp:event")
                        set("TIMEOUT", "Second-3600")
                        set("CALLBACK", "<$callbackUrl>")
                    }
                }
            }
            return response.headers["SID"].toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ""
    }

    /**
     * Renew UPnP device event subscription
     * @param device UPnP device
     * @param sid Subscription ID to renew
     * @return New subscription ID
     */
    suspend fun renewEvent(
        device: UPnPDevice,
        sid: String,
    ): String {
        val service = device.getAVTransportService() ?: return ""
        try {
            val client = HttpClient(CIO)
            val response = withIO {
                client.subscribe(device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/')) {
                    headers {
                        set("SID", sid)
                        set("TIMEOUT", "Second-3600")
                    }
                }
            }
            return response.headers["SID"].toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ""
    }

    /**
     * Unsubscribe from UPnP device events
     * @param device UPnP device
     * @param sid Subscription ID to cancel
     * @return Response XML content
     */
    suspend fun unsubscribeEvent(
        device: UPnPDevice,
        sid: String,
    ): String {
        val service = device.getAVTransportService() ?: return ""
        try {
            val client = HttpClient(CIO)
            val response = withIO {
                client.unsubscribe(device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/')) {
                    headers {
                        set("SID", sid)
                    }
                }
            }
            LogCat.e(response.toString())
            val xml = response.body<String>()
            LogCat.e(xml)
            if (response.status == HttpStatusCode.OK) {
                return xml
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ""
    }

    /**
     * HTTP client SUBSCRIBE method extension
     */
    private suspend inline fun HttpClient.subscribe(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return request(
            HttpRequestBuilder().apply {
                method = HttpMethod("SUBSCRIBE")
                url(urlString)
                block()
            },
        )
    }

    /**
     * HTTP client UNSUBSCRIBE method extension
     */
    private suspend inline fun HttpClient.unsubscribe(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return request(
            HttpRequestBuilder().apply {
                method = HttpMethod("UNSUBSCRIBE")
                url(urlString)
                block()
            },
        )
    }
} 