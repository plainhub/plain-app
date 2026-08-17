package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.lib.dlna.DlnaMediaUtils
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.xml.parseData
import com.ismartcoding.plain.lib.dlna.common.DlnaDevice
import com.ismartcoding.plain.lib.dlna.common.DlnaPositionInfoResponse
import com.ismartcoding.plain.lib.dlna.common.DlnaSoap
import com.ismartcoding.plain.lib.dlna.common.DlnaTransportInfoResponse
import com.ismartcoding.plain.platform.createHttpClient
import com.ismartcoding.plain.platform.getDeviceName
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode

object DlnaTransportController {

    suspend fun setAVTransportURIAsync(device: DlnaDevice, url: String, title: String = "", albumArtUri: String = ""): String {
        LogCat.e(url)
        val meta = if (title.isNotEmpty()) buildDidlLiteMetadata(url, title, albumArtUri) else ""
        return executeAVTransportCommand(
            device, "SetAVTransportURI",
            "<InstanceID>0</InstanceID><CurrentURI>$url</CurrentURI><CurrentURIMetaData>$meta</CurrentURIMetaData>",
        )
    }

    private fun buildDidlLiteMetadata(mediaUrl: String, title: String, albumArtUri: String): String {
        val didl = DlnaMediaUtils.buildDidlLite(mediaUrl, title, albumArtUri)
        return didl.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }

    suspend fun stopAVTransportAsync(device: DlnaDevice): String =
        executeAVTransportCommand(device, "Stop")

    suspend fun playAVTransportAsync(device: DlnaDevice): String =
        executeAVTransportCommand(device, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")

    suspend fun pauseAVTransportAsync(device: DlnaDevice): String =
        executeAVTransportCommand(device, "Pause")

    suspend fun seekAVTransportAsync(device: DlnaDevice, target: String): String =
        executeAVTransportCommand(
            device, "Seek",
            "<InstanceID>0</InstanceID><Unit>REL_TIME</Unit><Target>$target</Target>",
        )

    suspend fun getTransportInfoAsync(device: DlnaDevice): DlnaTransportInfoResponse {
        val st = device.getAVTransportService()?.serviceType ?: return DlnaTransportInfoResponse()
        val xml = executeSOAPRequest(
            device, "GetTransportInfo",
            "<u:GetTransportInfo xmlns:u=\"$st\"><InstanceID>0</InstanceID></u:GetTransportInfo>",
            logResponse = false,
        )
        return if (xml.isNotEmpty()) parseData(xml) else DlnaTransportInfoResponse()
    }

    suspend fun getPositionInfoAsync(device: DlnaDevice): DlnaPositionInfoResponse {
        val st = device.getAVTransportService()?.serviceType ?: return DlnaPositionInfoResponse()
        val xml = executeSOAPRequest(
            device, "GetPositionInfo",
            "<u:GetPositionInfo xmlns:u=\"$st\"><InstanceID>0</InstanceID></u:GetPositionInfo>",
        )
        return if (xml.isNotEmpty()) parseData(xml) else DlnaPositionInfoResponse()
    }

    suspend fun subscribeEvent(device: DlnaDevice, url: String): String =
        DlnaEventSubscriber.subscribeEvent(device, url)

    suspend fun renewEvent(device: DlnaDevice, sid: String): String =
        DlnaEventSubscriber.renewEvent(device, sid)

    suspend fun unsubscribeEvent(device: DlnaDevice, sid: String): String =
        DlnaEventSubscriber.unsubscribeEvent(device, sid)

    private suspend fun executeSOAPRequest(
        device: DlnaDevice,
        action: String,
        soapBody: String,
        logResponse: Boolean = true,
    ): String {
        val service = device.getAVTransportService() ?: return ""
        val senderName = TempData.deviceName.value.ifEmpty { getDeviceName() }
        return try {
            val client = createHttpClient()
            val response = withIO {
                client.post(device.getBaseUrl() + "/" + service.controlURL.trimStart('/')) {
                    headers {
                        set("Content-Type", "text/xml")
                        set("SOAPAction", "\"${service.serviceType}#$action\"")
                        set("c-name", senderName)
                    }
                    setBody(DlnaSoap.requestEnvelope(soapBody))
                }
            }
            if (logResponse) LogCat.e(response.toString())
            val xml = response.body<String>()
            if (logResponse) LogCat.e(xml)
            if (response.status == HttpStatusCode.OK) xml else ""
        } catch (ex: Exception) {
            ex.printStackTrace()
            ""
        }
    }

    private suspend fun executeAVTransportCommand(
        device: DlnaDevice,
        action: String,
        parameters: String = "<InstanceID>0</InstanceID>",
    ): String {
        val st = device.getAVTransportService()?.serviceType ?: return ""
        return executeSOAPRequest(device, action, "<u:$action xmlns:u=\"$st\">$parameters</u:$action>")
    }
}
