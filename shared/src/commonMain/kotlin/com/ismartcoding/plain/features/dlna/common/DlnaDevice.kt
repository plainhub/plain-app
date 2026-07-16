package com.ismartcoding.plain.features.dlna.common

import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.lib.xml.decodeXml
import kotlinx.serialization.Serializable

class DlnaDevice(
    val hostAddress: String,
    val header: String,
) {
    val location = parseHeader(header, "LOCATION: ")
    val server = parseHeader(header, "SERVER: ")
    val uSN = parseHeader(header, "USN: ")
    val sT = parseHeader(header, "ST: ")

    private var descriptionXML: String = ""
    var description: DescriptionModel? = null

    fun isAVTransport(): Boolean =
        description?.device?.serviceList?.any { it.serviceType == DlnaSoap.AVT_SERVICE_TYPE } == true

    fun update(xml: String) {
        descriptionXML = xml
        description = decodeXml(xml)
    }

    fun getAVTransportService(): DeviceService? =
        description?.device?.serviceList?.find { it.serviceId == "urn:upnp-org:serviceId:AVTransport" }

    /**
     * 从 location URL (如 "http://192.168.1.10:5000/desc.xml") 提取 base URL
     * ("http://192.168.1.10:5000")，不依赖 java.net.URL 以保持 KMP 兼容。
     */
    fun getBaseUrl(): String {
        // 移除协议前缀
        val rest = location.substringAfter("://", "")
        if (rest.isEmpty()) return ""
        // 找到第一个 '/' 或 '?' 之前的部分（host:port）
        val hostPort = rest.substringBefore('/')
        return "${location.substringBefore("://")}://$hostPort"
    }

    override fun toString(): String {
        var str = ""
        description?.device?.let { d -> str = jsonEncode(d) }
        return str
    }

    private fun parseHeader(mSearchAnswer: String, whatSearch: String): String {
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

    @Serializable
    data class DescriptionModel(val device: Device = Device(), val URLBase: String = "")

    @Serializable
    data class DeviceService(
        val serviceType: String = "",
        val serviceId: String = "",
        val controlURL: String = "",
        val eventSubURL: String = "",
        val SCPDURL: String = "",
    )
}
