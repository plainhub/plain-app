package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.callChatStore
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.crypto.ECDHKeyPair
import com.ismartcoding.plain.platform.getDeviceIP4s
import com.ismartcoding.plain.platform.getDeviceType
import com.ismartcoding.plain.platform.isWifiAwareSupported
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

object RustPairingStore {
    private fun device() = buildJsonObject {
        put("name", TempData.deviceName.value)
        put("port", UserPrefs.httpsPort.value)
        put("device_type", getDeviceType().name)
        put("ips", JsonArray(getDeviceIP4s().map(::JsonPrimitive)))
        put("aware_supported", isWifiAwareSupported)
    }
    suspend fun request(): Pair<DPairingRequest, ECDHKeyPair> {
        val result = callChatStore("pairingRequest") { put("device", device()) }.jsonObject
        return JsonHelper.jsonDecode<DPairingRequest>(result.getValue("request").toString()) to ECDHKeyPair(
            privateKeyEncoded = Base64Lenient.decode(result.getValue("privateKey").jsonPrimitive.content),
            publicKeyEncoded = Base64Lenient.decode(result.getValue("publicKey").jsonPrimitive.content),
        )
    }
    suspend fun response(request: DPairingRequest, accepted: Boolean): Pair<DPairingResponse, ECDHKeyPair?>? {
        val result = callChatStore("pairingResponse") {
            put("request", Json.parseToJsonElement(JsonHelper.jsonEncode(request)))
            put("accepted", accepted)
            put("device", device())
        }.takeUnless { it is JsonNull }?.jsonObject ?: return null
        val response = JsonHelper.jsonDecode<DPairingResponse>(result.getValue("response").toString())
        val key = result.getValue("privateKey").takeUnless { it is JsonNull }?.jsonPrimitive?.content?.let {
            ECDHKeyPair(privateKeyEncoded = Base64Lenient.decode(it), publicKeyEncoded = Base64Lenient.decode(response.ecdhPublicKey))
        }
        return response to key
    }
    suspend fun validateResponse(response: DPairingResponse, expected: String): Boolean = callChatStore("validatePairingResponse") {
        put("response", Json.parseToJsonElement(JsonHelper.jsonEncode(response)))
        put("expected", expected)
    }.jsonPrimitive.boolean
    suspend fun derive(privateKey: ByteArray, publicKey: ByteArray): String? = callChatStore("derivePairingKey") {
        put("private_key", Base64.encode(privateKey))
        put("public_key", Base64.encode(publicKey))
    }.takeUnless { it is JsonNull }?.jsonPrimitive?.content
    suspend fun save(id: String, name: String, ips: List<String>, port: Int, deviceType: DeviceType, key: String, publicKey: String) {
        callChatStore("savePairedPeer") {
            put("facts", buildJsonObject {
                put("id", id)
                put("name", name)
                put("ips", JsonArray(ips.map(::JsonPrimitive)))
                put("port", port)
                put("device_type", deviceType.name)
                put("key", key)
                put("public_key", publicKey)
            })
        }
    }
}
