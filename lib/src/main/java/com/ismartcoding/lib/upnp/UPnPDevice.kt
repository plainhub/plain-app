package com.ismartcoding.lib.upnp

import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.helpers.XmlHelper
import kotlinx.serialization.Serializable
import java.net.URL

class UPnPDevice(
    val hostAddress: String,
    val header: String,
) {
    val location = parseHeader(header, "LOCATION: ")
    val server = parseHeader(header, "SERVER: ")
    val uSN = parseHeader(header, "USN: ")
    val sT = parseHeader(header, "ST: ")

    private var descriptionXML: String = ""
    var description: DescriptionModel? = null

    fun isAVTransport(): Boolean {
        return description?.device?.serviceList?.any { it.serviceType == "urn:schemas-upnp-org:service:AVTransport:1" } == true
    }

    fun update(xml: String) {
        descriptionXML = xml
        description = XmlHelper.decodeXml(xml)
    }

    fun getAVTransportService(): DeviceService? {
        return description?.device?.serviceList?.find { it.serviceId == "urn:upnp-org:serviceId:AVTransport" }
    }

    fun getBaseUrl(): String {
        val url = URL(location)
        return url.protocol.toString() + "://" + url.host + ":" + url.port
    }

    override fun toString(): String {
        var str = ""
        description?.device?.let { d ->
            str = jsonEncode(d)
        }

        return str
    }

    private fun parseHeader(
        mSearchAnswer: String,
        whatSearch: String,
    ): String {
        var result = ""
        var searchLinePos = mSearchAnswer.indexOf(whatSearch)
        if (searchLinePos != -1) {
            searchLinePos += whatSearch.length
            val locColon = mSearchAnswer.indexOf("\n", searchLinePos)
            result = mSearchAnswer.substring(searchLinePos, locColon)
        }
        return result
    }

    @Serializable
    class Device {
        val deviceType: String = ""
        val friendlyName: String = ""
        val presentationURL: String = ""
        val serialNumber: String = ""
        val modelName: String = ""
        val modelNumber: String = ""
        val modelURL: String = ""
        val manufacturer: String = ""
        val manufacturerURL: String = ""
        val UDN: String = ""
        val serviceList: List<DeviceService> = listOf()
    }

    data class DescriptionModel(
        val device: Device,
        val URLBase: String,
    )

    @Serializable
    data class DeviceService(
        val serviceType: String,
        val serviceId: String,
        val controlURL: String,
        val eventSubURL: String,
        val SCPDURL: String,
    )
}
