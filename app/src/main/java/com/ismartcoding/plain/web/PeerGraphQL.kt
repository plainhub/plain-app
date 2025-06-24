package com.ismartcoding.plain.web

import android.util.Base64
import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.GraphqlRequest
import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.context
import com.apurebase.kgraphql.schema.Schema
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.dsl.SchemaConfigurationDSL
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.PeerChatHelper
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
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class PeerGraphQL(val schema: Schema) {
    class Configuration : SchemaConfigurationDSL() {
        fun init() {
            schemaBlock = {
                type<ChatItem> {
                    property("data") {
                        resolver { c: ChatItem ->
                            c.getContentData()
                        }
                    }
                }
                mutation("createChatItem") {
                    resolver { content: String, context: Context ->
                        val call = context.get<ApplicationCall>()!!
                        val clientId = call.request.header("c-id") ?: ""
                        val gid = call.request.header("c-gid") ?: ""
                        val item =
                            ChatHelper.sendAsync(
                                DChat.parseContent(content),
                                clientId,
                                "me"
                            )

                        if (item.content.type == DMessageType.TEXT.value) {
                            sendEvent(FetchLinkPreviewsEvent(item))
                        }
                        sendEvent(HttpApiEvents.MessageCreatedEvent(arrayListOf(item)))
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
        override val key = AttributeKey<PeerGraphQL>("KGraphQL")

        private suspend fun executeGraphqlQL(
            schema: Schema,
            query: String,
            call: ApplicationCall
        ): String {
            val request = Json.decodeFromString(GraphqlRequest.serializer(), query)
            return schema.execute(request.query, request.variables.toString(), context {
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
                        if (!TempData.webEnabled) {
                            call.respond(HttpStatusCode.Forbidden)
                            return@post
                        }
                        val clientId = call.request.header("c-id") ?: ""
                        val gid = call.request.header("c-gid") ?: ""
                        val token = if (gid.isNotEmpty()) ChatApiManager.groupKeyCache[gid] else ChatApiManager.peerKeyCache[clientId]
                        if (token == null) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@post
                        }

                        var decryptedStr = ""
                        val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, call.receive())
                        if (decryptedBytes != null) {
                            decryptedStr = decryptedBytes.decodeToString()
                        }
                        if (decryptedStr.isEmpty()) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@post
                        }

                        // Extract timestamp, signature and GraphQL JSON from decrypted string
                        // Format: "timestamp|signature|GraphQL_JSON"
                        var timestamp: String? = null
                        var signature: String? = null
                        var requestStr = decryptedStr
                        
                        if (decryptedStr.contains("|")) {
                            val parts = decryptedStr.split("|", limit = 3)
                            if (parts.size == 3) {
                                // Full format: timestamp|signature|GraphQL_JSON
                                timestamp = parts[0]
                                signature = parts[1]
                                requestStr = parts[2]
                                LogCat.d("[Request] Extracted timestamp: $timestamp, signature: ${signature?.take(10)}...")
                            } else if (parts.size == 2) {
                                // Legacy format: timestamp|GraphQL_JSON
                                timestamp = parts[0]
                                requestStr = parts[1]
                                LogCat.d("[Request] Extracted timestamp: $timestamp")
                            }
                        }

                        LogCat.d("[Request] GraphQL: $requestStr")
                        
                        // Verify peer message signature at route level for better security
                        if (signature != null && timestamp != null) {
                            val isValid = PeerChatHelper.verifyPeerMessageAsync(
                                peerId = clientId, 
                                content = requestStr, 
                                signature = signature, 
                                timestamp = timestamp.toLongOrNull() ?: 0L
                            )
                            if (!isValid) {
                                LogCat.e("Invalid signature from peer $clientId")
                                call.respond(HttpStatusCode.Unauthorized)
                                return@post
                            }
                            LogCat.d("Signature verified successfully for peer $clientId")
                        }
                        
                        ChatApiManager.clientRequestTs[clientId] = System.currentTimeMillis() // record the api request time
                        val r = executeGraphqlQL(schema, requestStr, call) // Signature already verified at route level
                        call.respondBytes(CryptoHelper.chaCha20Encrypt(token, r))
                    }
                }
            }
            return PeerGraphQL(schema)
        }
    }
}
