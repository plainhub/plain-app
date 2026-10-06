package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.mdns.MdnsServiceInfo
import kotlinx.serialization.json.*

object RustDiscoveryAdvertisement {
    private suspend fun call(action: String) = RustContentApi.postJsonOrThrow("chat/discovery", buildJsonObject { put("action", action) }).getValue("result")
    suspend fun replyJson() = call("reply")
    suspend fun reply(): DDiscoverReply = JsonHelper.jsonDecode(replyJson().toString())
    suspend fun ble(): ByteArray = Base64Lenient.decode(call("ble").jsonPrimitive.content)
    suspend fun mdns(): MdnsServiceInfo = call("mdns").jsonObject.let { row ->
        fun string(key: String) = row.getValue(key).jsonPrimitive.content
        fun strings(key: String) = row.getValue(key).jsonArray.map { it.jsonPrimitive.content }
        MdnsServiceInfo(string("instanceName"), string("serviceType"), string("targetHostname"), row.getValue("port").jsonPrimitive.int, strings("txtRecords"), strings("ips"))
    }
}
