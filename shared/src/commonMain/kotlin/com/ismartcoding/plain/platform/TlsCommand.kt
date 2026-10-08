package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@kotlinx.serialization.json.JsonClassDiscriminator("action")
@Serializable
internal sealed class TlsCommand {
    @Serializable @SerialName("get") data object Get : TlsCommand()
    @Serializable @SerialName("generate") data object Generate : TlsCommand()
    @Serializable @SerialName("import") data class Import(val certificatePem: String, val privateKeyPem: String) : TlsCommand()
    @Serializable @SerialName("importPkcs12") data class ImportPkcs12(val data: String, val password: String) : TlsCommand()
}
