package com.ismartcoding.lib.upnp

import android.util.Xml
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.XmlHelper.xmlDecode
import com.ismartcoding.lib.logcat.LogCat
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

object UPnPController {
    suspend fun setAVTransportURIAsync(
        device: UPnPDevice,
        url: String,
    ): String {
        val service = device.getAVTransportService() ?: return ""
        LogCat.e(url)
        try {
            val client = HttpClient(CIO)
            val response =
                withIO {
                    client.post(device.getBaseUrl() + "/" + service.controlURL.trimStart('/')) {
                        headers {
                            set("Content-Type", "text/xml")
                            set("SOAPAction", "\"${service.serviceType}#SetAVTransportURI\"")
                        }
                        setBody(
                            getRequestBody(
                                """
                                <u:SetAVTransportURI xmlns:u="${service.serviceType}">
                                    <InstanceID>0</InstanceID>
                                    <CurrentURI>$url</CurrentURI>
                                    <CurrentURIMetaData></CurrentURIMetaData>
                                </u:SetAVTransportURI>
                                """.trimIndent(),
                            ),
                        )
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

    suspend fun stopAVTransportAsync(
        device: UPnPDevice,
    ): String {
        val service = device.getAVTransportService() ?: return ""
        try {
            val client = HttpClient(CIO)
            val response =
                withIO {
                    client.post(device.getBaseUrl() + "/" + service.controlURL.trimStart('/')) {
                        headers {
                            set("Content-Type", "text/xml")
                            set("SOAPAction", "\"${service.serviceType}#Stop\"")
                        }
                        setBody(
                            getRequestBody(
                                """
                                <u:Stop xmlns:u="${service.serviceType}">
                                  <InstanceID>0</InstanceID>
                                </u:Stop>
                                """.trimIndent(),
                            ),
                        )
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

     suspend inline fun HttpClient.subscribe(
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

    public suspend inline fun HttpClient.unsubscribe(
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

    suspend fun subscribeEvent(
        device: UPnPDevice,
        url: String,
    ): String {
        val service = device.getAVTransportService() ?: return ""
        try {
            val client = HttpClient(CIO)
            val response =
                withIO {
                    client.subscribe(device.getBaseUrl() + "/" + service.eventSubURL.trimStart('/')) {
                        headers {
                            set("NT", "upnp:event")
                            set("TIMEOUT", "Second-3600")
                            set("CALLBACK", "<$url>")
                        }
                    }
                }
            return response.headers["SID"].toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ""
    }

    suspend fun renewEvent(
        device: UPnPDevice,
        sid: String,
    ): String {
        val service = device.getAVTransportService() ?: return ""
        try {
            val client = HttpClient(CIO)
            val response =
                withIO {
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

    suspend fun unsubscribeEvent(
        device: UPnPDevice,
        sid: String,
    ): String {
        val service = device.getAVTransportService() ?: return ""
        try {
            val client = HttpClient(CIO)
            val response =
                withIO {
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

    suspend fun getTransportInfoAsync(device: UPnPDevice): GetTransportInfoResponse {
        val service = device.getAVTransportService() ?: return GetTransportInfoResponse()
        val client = HttpClient(CIO)
        val response =
            withIO {
                client.post(device.getBaseUrl() + "/" + service.controlURL.trimStart('/')) {
                    headers {
                        set("Content-Type", "text/xml")
                        set("SOAPAction", "\"${service.serviceType}#GetTransportInfo\"")
                    }
                    setBody(
                        getRequestBody(
                            """
                            <u:GetTransportInfo xmlns:u="${service.serviceType}">
                                <InstanceID>0</InstanceID>
                            </u:GetTransportInfo>
                            """.trimIndent(),
                        ),
                    )
                }
            }
        val xml = response.body<String>()
        return parseData(xml)
    }

    private inline fun <reified T> parseData(xml: String): T {
        LogCat.d(xml)
        val stream = ByteArrayInputStream(xml.toByteArray())
        val parser = Xml.newPullParser()
        parser.setInput(stream, null)
        var eventType = parser.eventType
        var body = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_DOCUMENT -> {}
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("Body", ignoreCase = true)) {
                        body = parser.nextText()
                        break
                    }
                }
            }
            eventType = parser.next()
        }

        return xmlDecode<T>(body)
    }

    private fun getRequestBody(body: String): String {
        val xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <s:Envelope s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"
            	xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
            	<s:Body>
            		$body
            	</s:Body>
            </s:Envelope>
            """.trimIndent()
        LogCat.e(xml)
        return xml
    }
}
