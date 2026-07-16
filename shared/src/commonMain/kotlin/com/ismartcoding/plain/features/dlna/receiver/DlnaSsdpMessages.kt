package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.platform.getDeviceIP4

/**
 * Pure-Kotlin SSDP (Simple Service Discovery Protocol) message builders
 * for the DLNA MediaRenderer advertiser.
 *
 * Extracted from androidMain's `DlnaSsdpAdvertiser` so the message format
 * lives in commonMain; the platform layer only handles UDP multicast I/O.
 */
object DlnaSsdpMessages {
    const val SSDP_ADDR = "239.255.255.250"
    const val SSDP_PORT = 1900
    const val DEVICE_TYPE = "urn:schemas-upnp-org:device:MediaRenderer:1"
    const val AVT_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"

    /** NOTIFY ssdp:alive messages for root device, device type, and service. */
    fun aliveMessages(uuid: String): List<String> {
        return listOf(
            notifyMsg(uuid, "upnp:rootdevice", "ssdp:alive"),
            notifyMsg("$uuid::$DEVICE_TYPE", DEVICE_TYPE, "ssdp:alive"),
            notifyMsg("$uuid::$AVT_TYPE", AVT_TYPE, "ssdp:alive"),
        )
    }

    /** NOTIFY ssdp:byebye messages for root device, device type, and service. */
    fun byebyeMessages(uuid: String): List<String> {
        return listOf(
            notifyMsg(uuid, "upnp:rootdevice", "ssdp:byebye"),
            notifyMsg("$uuid::$DEVICE_TYPE", DEVICE_TYPE, "ssdp:byebye"),
            notifyMsg("$uuid::$AVT_TYPE", AVT_TYPE, "ssdp:byebye"),
        )
    }

    /** M-SEARCH response messages sent to unicast queriers. */
    fun searchResponses(uuid: String): List<String> {
        return listOf(
            searchResponse("upnp:rootdevice", "$uuid::upnp:rootdevice"),
            searchResponse(DEVICE_TYPE, "$uuid::$DEVICE_TYPE"),
            searchResponse(AVT_TYPE, "$uuid::$AVT_TYPE"),
        )
    }

    private fun notifyMsg(usn: String, nt: String, nts: String): String {
        val ip = getDeviceIP4()
        val port = DlnaRendererState.port.value
        return "NOTIFY * HTTP/1.1\r\nHOST: $SSDP_ADDR:$SSDP_PORT\r\n" +
            "CACHE-CONTROL: max-age=1800\r\nLOCATION: http://$ip:$port/description.xml\r\n" +
            "NT: $nt\r\nNTS: $nts\r\nSERVER: Android/1.0 UPnP/1.1 PlainApp/1.0\r\nUSN: $usn\r\n\r\n"
    }

    private fun searchResponse(st: String, usn: String): String {
        val ip = getDeviceIP4()
        val port = DlnaRendererState.port.value
        return "HTTP/1.1 200 OK\r\nCACHE-CONTROL: max-age=1800\r\n" +
            "LOCATION: http://$ip:$port/description.xml\r\n" +
            "SERVER: Android/1.0 UPnP/1.1 PlainApp/1.0\r\nST: $st\r\nUSN: $usn\r\n\r\n"
    }
}
