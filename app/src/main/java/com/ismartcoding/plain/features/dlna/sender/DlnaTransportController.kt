package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.XmlHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.dlna.common.DlnaDevice
import com.ismartcoding.plain.features.dlna.common.DlnaPositionInfoResponse
import com.ismartcoding.plain.features.dlna.common.DlnaSoap
import com.ismartcoding.plain.features.dlna.common.DlnaTransportInfoResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode

object DlnaTransportController {

    suspend fun setAVTransportURIAsync(device: DlnaDevice, url: String): String {
        LogCat.e(url)
        return executeAVTransportCommand(
            device, "SetAVTransportURI",
            "<InstanceID>0</InstanceID><CurrentURI>$url</CurrentURI><CurrentURIMetaData></CurrentURIMetaData>",
        )
    }

    suspend fun stopAVTransportAsync(device: DlnaDevice): String =
        executeAVTransportCommand(device, "Stop")

    suspend fun playAVTransportAsync(device: DlnaDevice): String =
        executeAVTransportCommand(device, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")

    suspend fun pauseAVTransportAsync(device: DlnaDevice): String =
        executeAVTransportCommand(device, "Pause")

    suspend fun getTransportInfoAsync(device: DlnaDevice): DlnaTransportInfoResponse {
        val st = device.getAVTransportService()?.serviceType ?: return DlnaTransportInfoResponse()
        val xml = executeSOAPRequest(
            device, "GetTransportInfo",
            "<u:GetTransportInfo xmlns:u=\"$st\"><InstanceID>0</InstanceID></u:GetTransportInfo>",
            logResponse = false,
        )
        return if (xml.isNotEmpty()) XmlHelper.parseData(xml) else DlnaTransportInfoResponse()
    }

    suspend fun getPositionInfoAsync(device: DlnaDevice): DlnaPositionInfoResponse {
        val st = device.getAVTransportService()?.serviceType ?: return DlnaPositionInfoResponse()
        val xml = executeSOAPRequest(
            device, "GetPositionInfo",
            "<u:GetPositionInfo xmlns:u=\"$st\"><InstanceID>0</InstanceID></u:GetPositionInfo>",
        )
        return if (xml.isNotEmpty()) XmlHelper.parseData(xml) else DlnaPositionInfoResponse()
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
        return try {
            val client = HttpClient(CIO)
            val response = withIO {
                client.post(device.getBaseUrl() + "/" + service.controlURL.trimStart('/')) {
                    headers {
                        set("Content-Type", "text/xml")
                        set("SOAPAction", "\"${service.serviceType}#$action\"")
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
