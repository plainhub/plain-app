package com.ismartcoding.plain.chat

import com.ismartcoding.plain.db.DMessageFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("action")
internal sealed class ChatServiceCommand {
    @Serializable @SerialName("send") data class Send(val target: String, val content: String) : ChatServiceCommand()
    @Serializable @SerialName("sendText") data class SendText(val targets: List<String>, val text: String) : ChatServiceCommand()
    @Serializable @SerialName("sendMany") data class SendMany(val targets: List<String>, val content: String) : ChatServiceCommand()
    @Serializable @SerialName("forward") data class Forward(val id: String, val target: String) : ChatServiceCommand()
    @Serializable @SerialName("share") data class Share(
        val targets: List<String>, val uris: List<String>, val text: String?, val caption: String?,
        val images: Boolean?, val normalize: Boolean,
    ) : ChatServiceCommand()
    @Serializable @SerialName("folder") data class Folder(
        val targets: List<String>, val path: String, val name: String,
        @SerialName("expires_at") val expiresAt: String?,
    ) : ChatServiceCommand()
    @Serializable @SerialName("shareContent") data class ShareContent(val id: String) : ChatServiceCommand()
    @Serializable @SerialName("retry") data class Retry(val id: String) : ChatServiceCommand()
    @Serializable @SerialName("delete") data class Delete(val ids: Set<String>) : ChatServiceCommand()
    @Serializable @SerialName("deleteQuery") data class DeleteQuery(val query: String) : ChatServiceCommand()
    @Serializable @SerialName("createFiles") data class CreateFiles(val target: String, val items: List<DMessageFile>, val images: Boolean) : ChatServiceCommand()
    @Serializable @SerialName("replaceFiles") data class ReplaceFiles(val id: String, val items: List<DMessageFile>) : ChatServiceCommand()
    @Serializable @SerialName("clear") data class Clear(val target: String) : ChatServiceCommand()
}
