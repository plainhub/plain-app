package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.RustCoreBridge
import kotlinx.serialization.json.*

object RustTlsCertificate {
    fun signature(): ByteArray = action("get")
    fun regenerate(): ByteArray = action("generate")
    fun importPem(certificate: String, key: String): ByteArray = action("import", certificate, key)
    private fun action(name: String, certificate: String = "", key: String = ""): ByteArray {
        val request = buildJsonObject {
            put("action", name)
            if (name == "import") { put("certificatePem", certificate); put("privateKeyPem", key) }
        }
        return Json.parseToJsonElement(RustCoreBridge.tls(request.toString())).jsonObject
            .getValue("signature").jsonArray.map { it.jsonPrimitive.int.toByte() }.toByteArray()
    }
}
