package com.ismartcoding.lib.upnp

import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.XmlHelper
import com.ismartcoding.lib.logcat.LogCat
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode

/**
 * UPnP Controller
 * Handles UPnP device AVTransport control functionality
 */
object UPnPController {
    
    // ==============================
    // AVTransport Control Methods
    // ==============================
    
    /**
     * Set AVTransport URI for playback
     * @param device UPnP device
     * @param url Media URL to play
     * @return Response XML content
     */
    suspend fun setAVTransportURIAsync(
        device: UPnPDevice,
        url: String,
    ): String {
        LogCat.e(url)
        val parameters = """
            <InstanceID>0</InstanceID>
            <CurrentURI>$url</CurrentURI>
            <CurrentURIMetaData></CurrentURIMetaData>
        """.trimIndent()
        
        return executeAVTransportCommand(device, "SetAVTransportURI", parameters)
    }

    /**
     * Stop AVTransport playback
     * @param device UPnP device
     * @return Response XML content
     */
    suspend fun stopAVTransportAsync(device: UPnPDevice): String {
        return executeAVTransportCommand(device, "Stop")
    }

    /**
     * Start AVTransport playback
     * @param device UPnP device
     * @return Response XML content
     */
    suspend fun playAVTransportAsync(device: UPnPDevice): String {
        val parameters = """
            <InstanceID>0</InstanceID>
            <Speed>1</Speed>
        """.trimIndent()
        
        return executeAVTransportCommand(device, "Play", parameters)
    }

    /**
     * Pause AVTransport playback
     * @param device UPnP device
     * @return Response XML content
     */
    suspend fun pauseAVTransportAsync(device: UPnPDevice): String {
        return executeAVTransportCommand(device, "Pause")
    }

    /**
     * Get transport information
     * @param device UPnP device
     * @return Transport info response
     */
    suspend fun getTransportInfoAsync(device: UPnPDevice): GetTransportInfoResponse {
        val xml = executeSOAPRequest(
            device, 
            "GetTransportInfo", 
            """
            <u:GetTransportInfo xmlns:u="${device.getAVTransportService()?.serviceType}">
                <InstanceID>0</InstanceID>
            </u:GetTransportInfo>
            """.trimIndent(),
            logResponse = false
        )
        
        return if (xml.isNotEmpty()) {
            XmlHelper.parseData(xml)
        } else {
            GetTransportInfoResponse()
        }
    }

    /**
     * Get playback position information
     * @param device UPnP device
     * @return Position info response
     */
    suspend fun getPositionInfoAsync(device: UPnPDevice): GetPositionInfoResponse {
        val xml = executeSOAPRequest(
            device,
            "GetPositionInfo",
            """
            <u:GetPositionInfo xmlns:u="${device.getAVTransportService()?.serviceType}">
                <InstanceID>0</InstanceID>
            </u:GetPositionInfo>
            """.trimIndent()
        )
        
        return if (xml.isNotEmpty()) {
            XmlHelper.parseData(xml)
        } else {
            GetPositionInfoResponse()
        }
    }

    // ==============================
    // Event Management Delegation Methods (Backward Compatibility)
    // ==============================
    
    /**
     * Subscribe to events - Delegates to UPnPEventManager
     */
    suspend fun subscribeEvent(device: UPnPDevice, url: String): String {
        return UPnPEventManager.subscribeEvent(device, url)
    }

    /**
     * Renew event subscription - Delegates to UPnPEventManager
     */
    suspend fun renewEvent(device: UPnPDevice, sid: String): String {
        return UPnPEventManager.renewEvent(device, sid)
    }

    /**
     * Unsubscribe from events - Delegates to UPnPEventManager
     */
    suspend fun unsubscribeEvent(device: UPnPDevice, sid: String): String {
        return UPnPEventManager.unsubscribeEvent(device, sid)
    }

    // ==============================
    // Internal Helper Methods
    // ==============================
    
    /**
     * Execute generic SOAP request
     * @param device UPnP device
     * @param action SOAP action name
     * @param soapBody SOAP request body
     * @param logResponse Whether to log response
     * @return Response XML content
     */
    private suspend fun executeSOAPRequest(
        device: UPnPDevice,
        action: String,
        soapBody: String,
        logResponse: Boolean = true
    ): String {
        val service = device.getAVTransportService() ?: return ""
        try {
            val client = HttpClient(CIO)
            val response = withIO {
                client.post(device.getBaseUrl() + "/" + service.controlURL.trimStart('/')) {
                    headers {
                        set("Content-Type", "text/xml")
                        set("SOAPAction", "\"${service.serviceType}#$action\"")
                    }
                    setBody(getRequestBody(soapBody))
                }
            }
            
            if (logResponse) {
                LogCat.e(response.toString())
            }
            
            val xml = response.body<String>()
            if (logResponse) {
                LogCat.e(xml)
            }
            
            if (response.status == HttpStatusCode.OK) {
                return xml
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ""
    }

    /**
     * Execute AVTransport control command
     * @param device UPnP device
     * @param action Command action name
     * @param parameters Command parameters
     * @return Response XML content
     */
    private suspend fun executeAVTransportCommand(
        device: UPnPDevice,
        action: String,
        parameters: String = "<InstanceID>0</InstanceID>"
    ): String {
        val soapBody = """
            <u:$action xmlns:u="${device.getAVTransportService()?.serviceType}">
                $parameters
            </u:$action>
        """.trimIndent()
        
        return executeSOAPRequest(device, action, soapBody)
    }

    /**
     * Build SOAP request body
     * @param body Inner SOAP body content
     * @return Complete SOAP envelope
     */
    private fun getRequestBody(body: String): String {
        val xml = """
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
