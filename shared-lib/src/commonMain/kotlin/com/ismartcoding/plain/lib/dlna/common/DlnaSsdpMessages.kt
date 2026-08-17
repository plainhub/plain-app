package com.ismartcoding.plain.lib.dlna.common

/**
 * Pure-Kotlin SSDP (Simple Service Discovery Protocol) message builders
 * for the DLNA MediaRenderer advertiser and scanner.
 *
 * The message format lives in shared-lib; callers pass in the local IP and
 * HTTP port so no platform or app-state dependency is needed here.
 */
object DlnaSsdpMessages {
    const val SSDP_ADDR = "239.255.255.250"
    const val SSDP_PORT = 1900
    const val DEVICE_TYPE = "urn:schemas-upnp-org:device:MediaRenderer:1"
    const val AVT_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"

    /** M-SEARCH query used by the DLNA device scanner to discover renderers. */
    const val M_SEARCH_QUERY =
        "M-SEARCH * HTTP/1.1\r\nST: ssdp:all\r\nHOST: $SSDP_ADDR:$SSDP_PORT\r\nMX: 3\r\nMAN: \"ssdp:discover\"\r\n\r\n"

    /** NOTIFY ssdp:alive messages for root device, device type, and service. */
    fun aliveMessages(uuid: String, ip: String, port: Int): List<String> {
        return listOf(
            notifyMsg(uuid, "upnp:rootdevice", "ssdp:alive", ip, port),
            notifyMsg("$uuid::$DEVICE_TYPE", DEVICE_TYPE, "ssdp:alive", ip, port),
            notifyMsg("$uuid::$AVT_TYPE", AVT_TYPE, "ssdp:alive", ip, port),
        )
    }

    /** NOTIFY ssdp:byebye messages for root device, device type, and service. */
    fun byebyeMessages(uuid: String, ip: String, port: Int): List<String> {
        return listOf(
            notifyMsg(uuid, "upnp:rootdevice", "ssdp:byebye", ip, port),
            notifyMsg("$uuid::$DEVICE_TYPE", DEVICE_TYPE, "ssdp:byebye", ip, port),
            notifyMsg("$uuid::$AVT_TYPE", AVT_TYPE, "ssdp:byebye", ip, port),
        )
    }

    /** M-SEARCH response messages sent to unicast queriers. */
    fun searchResponses(uuid: String, ip: String, port: Int): List<String> {
        return listOf(
            searchResponse("upnp:rootdevice", "$uuid::upnp:rootdevice", ip, port),
            searchResponse(DEVICE_TYPE, "$uuid::$DEVICE_TYPE", ip, port),
            searchResponse(AVT_TYPE, "$uuid::$AVT_TYPE", ip, port),
        )
    }

    private fun notifyMsg(usn: String, nt: String, nts: String, ip: String, port: Int): String {
        return "NOTIFY * HTTP/1.1\r\nHOST: $SSDP_ADDR:$SSDP_PORT\r\n" +
            "CACHE-CONTROL: max-age=1800\r\nLOCATION: http://$ip:$port/description.xml\r\n" +
            "NT: $nt\r\nNTS: $nts\r\nSERVER: Android/1.0 UPnP/1.1 PlainApp/1.0\r\nUSN: $usn\r\n\r\n"
    }

    private fun searchResponse(st: String, usn: String, ip: String, port: Int): String {
        return "HTTP/1.1 200 OK\r\nCACHE-CONTROL: max-age=1800\r\n" +
            "LOCATION: http://$ip:$port/description.xml\r\n" +
            "SERVER: Android/1.0 UPnP/1.1 PlainApp/1.0\r\nST: $st\r\nUSN: $usn\r\n\r\n"
    }
}
