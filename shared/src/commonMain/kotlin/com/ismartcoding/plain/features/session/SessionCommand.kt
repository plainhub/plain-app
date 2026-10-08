package com.ismartcoding.plain.features.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@kotlinx.serialization.json.JsonClassDiscriminator("action")
@Serializable
internal sealed class SessionCommand {
    @Serializable @SerialName("key") data class Key(val clientId: String) : SessionCommand()
    @Serializable @SerialName("list") data object List : SessionCommand()
    @Serializable @SerialName("create") data class Create(val name: String) : SessionCommand()
    @Serializable @SerialName("rename") data class Rename(val clientId: String, val name: String) : SessionCommand()
    @Serializable @SerialName("delete") data class Delete(val clientId: String) : SessionCommand()
}
