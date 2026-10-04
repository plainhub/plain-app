package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.chat.peer.RustPeerWireStore
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.kgraphql.GraphqlRequest
import com.ismartcoding.plain.lib.kgraphql.KGraphQL
import com.ismartcoding.plain.lib.kgraphql.context
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedPeerResolvers
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedSchema
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.httpserver.http.GraphqlRequestContext
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpStatus
import com.ismartcoding.plain.httpserver.models.ChatItem
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.mainschemas.addPeerSchemaTypes
import kotlinx.serialization.json.*
import kotlin.reflect.typeOf

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
     * signature/timestamp in shared Rust, then execute the GraphQL
     * operation and re-encrypt the response with the same key.
     */
    suspend fun handle(call: HttpCall) {
        if (!UserPrefs.service.value) {
            LogCat.w("[PeerGraphQL] reject webDisabled")
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return
        }

        val clientId = call.header("c-id") ?: ""
        val channelId = call.header("c-cid") ?: ""
        LogCat.d("[PeerGraphQL] from=$clientId channelId=$channelId")

        val authenticated = RustPeerWireStore.authenticatePeer(clientId, channelId, call.receiveBody())
        val status = authenticated.getValue("status").jsonPrimitive.int
        if (status != HttpStatus.OK) {
            call.respondNoBody(status)
            return
        }
        val token = Base64Lenient.decode(authenticated.getValue("key").jsonPrimitive.content)
        val content = authenticated.getValue("content").jsonPrimitive.content
        val ctxHolder = GraphqlRequestContext(call).apply {
            setAttribute(ATTR_SIGNATURE, authenticated.getValue("signature").jsonPrimitive.content)
            setAttribute(ATTR_TIMESTAMP, authenticated.getValue("timestamp").jsonPrimitive.long)
        }
        val request = Json.decodeFromString(GraphqlRequest.serializer(), content)
        val ctx = context { +ctxHolder }
        val result = withIO { schema.execute(request.query, request.variables?.toString(), ctx) }
        call.respond(
            RustPeerWireStore.encrypt(token, result),
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
         * results with [com.ismartcoding.plain.httpserver.models.ID] and
         * [kotlin.time.Instant] fields) with the peer-specific mutations.
         */
        fun create(): PeerGraphQLService {
            val schema = KGraphQL.schema {
                registerGeneratedSchema()
                addPeerSchemaTypes()
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
                property("fromId", typeOf<ID>(), { it: ChatItem -> ID(it.fromId) })
                property("toId", typeOf<ID>(), { it: ChatItem -> ID(it.toId) })
                property("channelId", typeOf<ID?>(), { it: ChatItem -> it.channelId.ifEmpty { null }?.let { id -> ID(id) } })
            }
        }
    }
}
