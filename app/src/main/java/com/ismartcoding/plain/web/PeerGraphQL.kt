package com.ismartcoding.plain.web

import android.annotation.SuppressLint
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.KGraphQL
import com.ismartcoding.plain.lib.kgraphql.context
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaConfigurationDSL
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.channel.ChannelSystemMessageReceiver
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.ChatMessageReceiver
import com.ismartcoding.plain.chat.ReplayedMessageException
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerChatParser
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.lib.kgraphql.GraphqlRequest
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
                        ChannelSystemMessageReceiver.handle(fromId, type, payload)
                        true
                    }
                }
                mutation("createChatItem") {
                    resolver { content: String, context: Context ->
                        val call = context.get<ApplicationCall>()!!
                        val fromPeerId = call.request.header("c-id") ?: ""
                        val fromChannelId = call.request.header("c-cid") ?: ""
                        val signature = call.attributes.getOrNull(SignatureAttrKey) ?: ""
                        val timestamp = call.attributes.getOrNull(TimestampAttrKey) ?: 0L

                        val item = try {
                            ChatMessageReceiver.receive(
                                fromPeerId = fromPeerId,
                                content = DChat.parseContent(content),
                                fromChannelId = fromChannelId,
                                signature = signature,
                                timestamp = timestamp,
                            )
                        } catch (e: ReplayedMessageException) {
                            LogCat.d("Dropped replayed message from $fromPeerId")
                            return@resolver emptyList<ChatItem>()
                        }
                        listOf(item.toModel())
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
        private val SignatureAttrKey = AttributeKey<String>("PeerGraphQL.Signature")
        private val TimestampAttrKey = AttributeKey<Long>("PeerGraphQL.Timestamp")

        private suspend fun executeGraphqlQL(
            schema: Schema,
            query: String,
            call: ApplicationCall
        ): String = withIO {
            val request = Json.decodeFromString(GraphqlRequest.serializer(), query)
            schema.execute(request.query, request.variables?.toString(), context {
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
                            ChannelCacher.getKeyBytes(channelId)
                        } else {
                            PeerCacher.getKeyBytes(clientId)
                        }
                        val publicKey = PeerCacher.getPublicKeyBytes(clientId)
                        if (token == null || publicKey == null) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@post
                        }
                        val decryptResult = PeerChatParser.decrypt(token, clientId, publicKey, call.receive())
                        if (decryptResult.content == null) {
                            call.respond(decryptResult.code)
                            return@post
                        }

                        call.attributes.put(SignatureAttrKey, decryptResult.signature)
                        call.attributes.put(TimestampAttrKey, decryptResult.timestamp)

                        val r = executeGraphqlQL(schema, decryptResult.content!!, call)
                        call.respondBytes(CryptoHelper.chaCha20Encrypt(token, r))
                    }
                }
            }
            return PeerGraphQL(schema)
        }
    }
}
