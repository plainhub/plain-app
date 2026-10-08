package com.ismartcoding.plain.features.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@kotlinx.serialization.json.JsonClassDiscriminator("action")
@Serializable
internal sealed class WebLoginCommand {
    @Serializable @SerialName("cancel") data class Cancel(val requestId: String) : WebLoginCommand()
    @Serializable @SerialName("complete") data class Complete(val requestId: String) : WebLoginCommand()
    @Serializable @SerialName("resetPassword") data object ResetPassword : WebLoginCommand()
}
