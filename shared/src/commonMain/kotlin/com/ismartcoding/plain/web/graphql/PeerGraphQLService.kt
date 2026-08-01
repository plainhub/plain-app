package com.ismartcoding.plain.web.graphql

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChatMessageReceiver
import com.ismartcoding.plain.chat.ReplayedMessageException
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.channel.ChannelSystemMessageReceiver
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerChatParser
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.GraphqlRequest
import com.ismartcoding.plain.lib.kgraphql.KGraphQL
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLSchemaTarget
import com.ismartcoding.plain.lib.kgraphql.context
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedPeerResolvers
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedSchema
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.startAwareIfNeeded
import com.ismartcoding.plain.platform.subscribeAwareForPeer
import com.ismartcoding.plain.web.http.GraphqlRequestContext
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpStatus
import com.ismartcoding.plain.web.models.ChatItem
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.schemas.addSchemaTypes
import kotlinx.serialization.json.Json

@GraphQLMutation(target = GraphQLSchemaTarget.PEER)
suspend fun channelSystemMessage(type: String, payload: String, context: Context): Boolean {
    val ctx = context.get<GraphqlRequestContext>()!!
    val fromId = ctx.header("c-id") ?: ""
    ChannelSystemMessageReceiver.handle(fromId, type, payload)
    return true
}

@GraphQLMutation(target = GraphQLSchemaTarget.PEER)
suspend fun createChatItem(content: String, context: Context): List<ChatItem> {
    val ctx = context.get<GraphqlRequestContext>()!!
    val fromPeerId = ctx.header("c-id") ?: ""
    val fromChannelId = ctx.header("c-cid") ?: ""
    val signature: String = ctx.attribute(PeerGraphQLService.ATTR_SIGNATURE) ?: ""
    val timestamp: Long = ctx.attribute(PeerGraphQLService.ATTR_TIMESTAMP) ?: 0L

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
        return emptyList()
    }
    return listOf(item.toModel())
}

@GraphQLMutation(target = GraphQLSchemaTarget.PEER)
suspend fun startAware(context: Context): Boolean {
    val ctx = context.get<GraphqlRequestContext>()!!
    val fromPeerId = ctx.header("c-id") ?: ""
    LogCat.d("[PeerGraphQL] startAware from=$fromPeerId")
    val started = startAwareIfNeeded()
    if (started) {
        withIO {
            val peer = AppDatabase.instance.peerDao().getById(fromPeerId)
            if (peer != null) {
                subscribeAwareForPeer(peer)
            }
        }
    }
    return started
}

/**
 * Holds the peer-chat GraphQL [Schema] and dispatches `/peer_graphql`
 * requests.
 *
 * The schema extends the main schema with two mutations used for peer-to-peer
 * chat and channel system messages. The resolvers read request headers from
 * the shared [GraphqlRequestContext] (instead of Ktor's `ApplicationCall`),
 * keeping them commonMain-compatible.
 */
class PeerGraphQLService private constructor(
    val schema: Schema,
) {
    /**
     * Decrypt the peer-encrypted request body using either the channel key
     * (when `c-cid` is present) or the peer's shared key, validate the
     * signature/timestamp via [PeerChatParser], then execute the GraphQL
     * operation and re-encrypt the response with the same key.
     */
    suspend fun handle(call: HttpCall) {
        if (!TempData.webEnabled.value) {
            LogCat.w("[PeerGraphQL] reject webDisabled")
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return
        }

        val clientId = call.header("c-id") ?: ""
        val channelId = call.header("c-cid") ?: ""
        LogCat.d("[PeerGraphQL] from=$clientId channelId=$channelId")

        // Determine the decryption key:
        // 1. If c-cid is present, always use the channel key (supports non-paired members).
        // 2. Otherwise, use the peer's shared key (paired peer-to-peer chat).
        val token = if (channelId.isNotEmpty()) {
            ChannelCacher.getKeyBytes(channelId)
        } else {
            // Direct peer-to-peer message: require the peer to be paired.
            // An unpaired peer must not be able to deliver messages to us.
            val peer = PeerCacher.getPeer(clientId)
            if (peer == null || !peer.isPaired()) {
                LogCat.w("[PeerGraphQL] reject non-paired direct message from=$clientId status=${peer?.status}")
                call.respondNoBody(HttpStatus.FORBIDDEN)
                return
            }
            PeerCacher.getKeyBytes(clientId)
        }
        val publicKey = PeerCacher.getPublicKeyBytes(clientId)
        if (token == null || publicKey == null) {
            LogCat.w("[PeerGraphQL] unauthorized from=$clientId token=${token != null} pub=${publicKey != null}")
            call.respondNoBody(HttpStatus.UNAUTHORIZED)
            return
        }

        val decryptResult = PeerChatParser.decrypt(token, clientId, publicKey, call.receiveBody())
        if (decryptResult.content == null) {
            LogCat.w("[PeerGraphQL] decrypt fail from=$clientId code=${decryptResult.code}")
            call.respondNoBody(decryptResult.code.value)
            return
        }

        // Carry the verified signature and timestamp to the resolvers via the
        // shared request context. The resolvers read them back through
        // GraphqlRequestContext.attribute(...).
        val ctxHolder = GraphqlRequestContext(call).apply {
            setAttribute(ATTR_SIGNATURE, decryptResult.signature)
            setAttribute(ATTR_TIMESTAMP, decryptResult.timestamp)
        }

        val request = Json.decodeFromString(GraphqlRequest.serializer(), decryptResult.content)
        val ctx = context { +ctxHolder }
        val result = withIO { schema.execute(request.query, request.variables?.toString(), ctx) }
        call.respond(
            chaCha20Encrypt(token, result),
            contentType = "application/octet-stream",
        )
        LogCat.d("[PeerGraphQL] done from=$clientId")
    }

    companion object {
        const val ATTR_SIGNATURE = "peerGraphql.signature"
        const val ATTR_TIMESTAMP = "peerGraphql.timestamp"

        /**
         * Build the [PeerGraphQLService] with a schema that combines the
         * shared scalar/enum types (so peer mutations can return [ChatItem]
         * results with [com.ismartcoding.plain.web.models.ID] and
         * [kotlin.time.Instant] fields) with the peer-specific mutations.
         */
        fun create(): PeerGraphQLService {
            val schema = KGraphQL.schema {
                registerGeneratedSchema()
                addSchemaTypes()
                applyPeerSchema()
            }
            return PeerGraphQLService(schema)
        }

        /**
         * Schema block that adds the peer-chat mutations on top of the main
         * schema. The resolvers reach the request headers via the
         * [GraphqlRequestContext] injected into the KGraphQL Context.
         */
        fun SchemaBuilder.applyPeerSchema() {
            registerGeneratedPeerResolvers()
            type<ChatItem> {
                property("data") {
                    resolver { c: ChatItem -> c.getContentData() }
                }
            }
        }
    }
}
