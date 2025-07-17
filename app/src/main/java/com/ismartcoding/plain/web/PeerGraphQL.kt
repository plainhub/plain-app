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
import com.ismartcoding.plain.chat.DownloadQueue
import com.ismartcoding.plain.chat.PeerChatHelper.MAX_TIMESTAMP_DIFF_MS
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.features.ChatHelper
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
import kotlin.math.abs

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

                        val fromId = call.request.header("c-id") ?: ""
                        val gid = call.request.header("c-gid") ?: ""
                        val item =
                            ChatHelper.sendAsync(
                                DChat.parseContent(content),
                                fromId,
                                "me"
                            )

                        if (item.content.type == DMessageType.TEXT.value) {
                            sendEvent(FetchLinkPreviewsEvent(item))
                        }

                        // Download files from peer automatically using queue
                        if (setOf(DMessageType.FILES.value, DMessageType.IMAGES.value).contains(item.content.type)) {
                            val files = when (item.content.value) {
                                is DMessageFiles -> (item.content.value as DMessageFiles).items
                                is DMessageImages -> (item.content.value as DMessageImages).items
                                else -> emptyList()
                            }

                            val peer = AppDatabase.instance.peerDao().getById(fromId)
                            if (peer != null) {
                                // Add files to download queue instead of downloading directly
                                files.forEach { file ->
                                    DownloadQueue.addDownloadTask(
                                        messageFile = file,
                                        peer = peer,
                                        messageId = item.id
                                    )
                                }
                            }
                        }

                        sendEvent(HttpApiEvents.MessageCreatedEvent(fromId, arrayListOf(item)))
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

                        val publicKey = ChatApiManager.peerPublicKeyCache[clientId]
                        if (publicKey == null) {
                            LogCat.e("Peer public key not found for clientId: $clientId")
                            call.respond(HttpStatusCode.InternalServerError)
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
                        // Format: "signature|timestamp|GraphQL_JSON"
                        var timestamp = 0L
                        var signature = ""
                        var requestStr = ""

                        if (decryptedStr.contains("|")) {
                            val parts = decryptedStr.split("|", limit = 3)
                            signature = parts[0]
                            timestamp = parts[1].toLongOrNull() ?: 0L
                            requestStr = parts[2]
                        }

                        LogCat.d("[Request] GraphQL: $requestStr")

                        val currentTime = System.currentTimeMillis()
                        // Verify timestamp is within acceptable range, replay attacks are not allowed
                        if (abs(currentTime - timestamp) > MAX_TIMESTAMP_DIFF_MS) {
                            LogCat.e("Message timestamp is too old or in the future: $timestamp - rejected")
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }

                        val signatureBytes = Base64.decode(signature, Base64.NO_WRAP)
                        val messageBytes = "$timestamp$requestStr".toByteArray()
                        val isValid = CryptoHelper.verifySignatureWithRawEd25519PublicKey(publicKey, messageBytes, signatureBytes)
                        if (!isValid) {
                            LogCat.e("Invalid signature from peer $clientId")
                            call.respond(HttpStatusCode.Unauthorized)
                            return@post
                        }

                        ChatApiManager.clientRequestTs[clientId] = currentTime

                        val r = executeGraphqlQL(schema, requestStr, call) // Signature already verified at route level
                        call.respondBytes(CryptoHelper.chaCha20Encrypt(token, r))
                    }
                }
            }
            return PeerGraphQL(schema)
        }
    }
}
