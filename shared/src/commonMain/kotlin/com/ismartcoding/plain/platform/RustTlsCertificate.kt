package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.RustContentApi
import kotlinx.serialization.json.jsonObject
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper

object RustTlsCertificate {
    suspend fun signature(): ByteArray = action(TlsCommand.Get)
    suspend fun regenerate(): ByteArray = action(TlsCommand.Generate)
    suspend fun importPem(certificate: String, key: String): ByteArray = action(TlsCommand.Import(certificate, key))
    suspend fun importPkcs12(bytes: ByteArray, password: String): ByteArray = action(TlsCommand.ImportPkcs12(Base64Lenient.encode(bytes), password))
    private suspend fun action(command: TlsCommand): ByteArray = JsonHelper.jsonDecode<TlsSignatureFacts>(
        RustContentApi.postJsonOrThrow("system/tls", JsonHelper.jsonEncodeToElement<TlsCommand>(command).jsonObject).toString()).signature.map(Int::toByte).toByteArray()
}
