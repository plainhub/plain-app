package com.ismartcoding.plain.web

import android.annotation.SuppressLint
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.kgraphql.Context
import com.ismartcoding.lib.kgraphql.GraphqlRequest
import com.ismartcoding.lib.kgraphql.KGraphQL
import com.ismartcoding.lib.kgraphql.context
import com.ismartcoding.lib.kgraphql.schema.Schema
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaConfigurationDSL
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.channel.ChannelSystemMessageHandler
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.download.DownloadQueue
import com.ismartcoding.plain.chat.peer.PeerChatParser
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.getMessagePreview
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HMessageCreatedEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.peer_chat
import com.ismartcoding.plain.web.models.ChatItem
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class PeerGraphQL(val schema: Schema) {
    class Configuration : SchemaConfigurationDSL() {
        @SuppressLint("MissingPermission")
        fun init() {
            schemaBlock = {
                type<ChatItem> {
                    property("data") {
                        resolver { c: ChatItem ->
                            c.getContentData()
                        }
                    }
                }
                mutation("channelSystemMessage") {
                    resolver { type: String, payload: String, context: Context ->
                        val call = context.get<ApplicationCall>()!!
                        val fromId = call.request.header("c-id") ?: ""
                        ChannelSystemMessageHandler.handle(fromId, type, payload)
                        true
                    }
                }
                mutation("createChatItem") {
                    resolver @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS) { content: String, context: Context ->
                        val call = context.get<ApplicationCall>()!!

                        val fromPeerId = call.request.header("c-id") ?: ""
                        val fromChannelId = call.request.header("c-cid") ?: ""

                        val fromPeer = AppDatabase.instance.peerDao().getById(fromPeerId) ?: throw Exception("invalid peer")

                        // Reject channel messages if we have left or been kicked
                        var fromChannel: DChatChannel? = null
                        if (fromChannelId.isNotEmpty()) {
                            fromChannel = AppDatabase.instance.chatChannelDao().getById(fromChannelId)
                            if (fromChannel == null || fromChannel.status != DChatChannel.STATUS_JOINED) {
                                throw IllegalStateException("Channel not joined")
                            }
                        }

                        val item = ChatDbHelper.insertChatItem(
                            DChat.parseContent(content),
                            fromPeerId,
                            toId = if (fromChannelId.isEmpty()) "me" else "",
                            channelId = fromChannelId,
                            isRemote = false
                        )

                        if (item.content.type == DMessageType.TEXT.value) {
                            sendEvent(FetchLinkPreviewsEvent(item))
                        }

                        // Download files from peer automatically using queue
                        if (setOf(
                                DMessageType.FILES.value,
                                DMessageType.IMAGES.value
                            ).contains(item.content.type)
                        ) {
                            val files = when (item.content.value) {
                                is DMessageFiles -> (item.content.value as DMessageFiles).items
                                is DMessageImages -> (item.content.value as DMessageImages).items
                                else -> emptyList()
                            }

                            // Add files to download queue instead of downloading directly
                            files.forEach { file ->
                                DownloadQueue.addDownloadTask(
                                    messageFile = file,
                                    peer = fromPeer,
                                    messageId = item.id
                                )
                            }
                        }

                        sendEvent(
                            HMessageCreatedEvent(
                                if (fromChannelId.isNotEmpty()) ChatTarget(fromChannelId, ChatTargetType.CHANNEL)
                                else ChatTarget(fromPeerId, ChatTargetType.PEER),
                                arrayListOf(item),
                            )
                        )
                        val model = item.toModel()
                        model.data = model.getContentData()
                        sendEvent(
                            WebSocketEvent(
                                EventType.MESSAGE_CREATED,
                                JsonHelper.jsonEncode(listOf(model))
                            )
                        )

                        if (Permission.POST_NOTIFICATIONS.can(MainApp.instance)) {
                            val preview = item.getMessagePreview()
                            val (targetId, targetName, messageText) = if (fromChannel == null) {
                                NotificationPayload(
                                    targetId = "peer:$fromPeerId",
                                    targetName = fromPeer.name.ifEmpty { LocaleHelper.getStringSync(Res.string.peer_chat) },
                                    messageText = preview,
                                )
                            } else {
                                NotificationPayload(
                                    targetId = "channel:$fromChannel",
                                    targetName = fromChannel.name.ifEmpty { LocaleHelper.getStringSync(Res.string.peer_chat) },
                                    messageText = "${fromPeer.name}: $preview",
                                )
                            }
                            if (ChatCacheManager.activeToId != targetId) {
                                NotificationHelper.sendChatMessageNotification(
                                    context = MainApp.instance,
                                    targetId = targetId,
                                    targetName = targetName,
                                    messageText = messageText,
                                )
                            }
                        }

                        arrayListOf(item).map { it.toModel() }
                    }
                }
                stringScalar<Instant> {
                    deserialize = { value: String -> Instant.parse(value) }
                    serialize = Instant::toString
                }

                stringScalar<ID> {
                    deserialize = { it: String -> ID(it) }
                    serialize = { it: ID -> it.toString() }
                }
            }
        }

        internal var schemaBlock: (SchemaBuilder.() -> Unit)? = null
    }

    companion object Feature : BaseApplicationPlugin<Application, Configuration, PeerGraphQL> {
        override val key = AttributeKey<PeerGraphQL>("PeerGraphQL")

        private suspend fun executeGraphqlQL(
            schema: Schema,
            query: String,
            call: ApplicationCall
        ): String {
            val request = Json.decodeFromString(GraphqlRequest.serializer(), query)
            return schema.execute(request.query, request.variables?.toString(), context {
                +call
            })
        }

        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit,
        ): PeerGraphQL {
            val config = Configuration().apply(configure)
            val schema =
                KGraphQL.schema {
                    configuration = config
                    config.schemaBlock?.invoke(this)
                }

            pipeline.routing {
                route("/peer_graphql") {
                    post {
                        if (!TempData.webEnabled.value) {
                            call.respond(HttpStatusCode.Forbidden)
                            return@post
                        }
                        val clientId = call.request.header("c-id") ?: ""
                        val channelId = call.request.header("c-cid") ?: ""
                        // Determine the decryption key:
                        // 1. If c-cid is present, always use the channel key (supports non-paired members).
                        // 2. Otherwise, use the peer's shared key (paired peer-to-peer chat).
                        val token = if (channelId.isNotEmpty()) {
                            ChatCacheManager.channelKeyCache[channelId]
                        } else {
                            ChatCacheManager.peerKeyCache[clientId]
                        }
                        val publicKey = ChatCacheManager.peerPublicKeyCache[clientId]
                        if (token == null || publicKey == null) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@post
                        }
                        val decryptResult = PeerChatParser.decrypt(token, clientId, publicKey, call.receive())
                        if (decryptResult.content == null) {
                            call.respond(decryptResult.code)
                            return@post
                        }

                        val r = executeGraphqlQL(schema, decryptResult.content, call)
                        call.respondBytes(CryptoHelper.chaCha20Encrypt(token, r))
                    }
                }
            }
            return PeerGraphQL(schema)
        }
    }
}

private data class NotificationPayload(
    val targetId: String,
    val targetName: String,
    val messageText: String,
)
