package com.ismartcoding.plain.web.graphql

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.events.WebRequestReceivedEvent
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.GraphqlRequest
import com.ismartcoding.plain.lib.kgraphql.KGraphQL
import com.ismartcoding.plain.lib.kgraphql.context
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.ReplayGuard
import com.ismartcoding.plain.web.SessionList
import com.ismartcoding.plain.web.http.GraphqlRequestContext
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpStatus
import com.ismartcoding.plain.web.serialize
import com.ismartcoding.plain.web.schemas.applyMainSchema
import kotlinx.serialization.json.Json

/**
 * Holds the main GraphQL [Schema] and exposes the route handlers that run
 * against it. The platform layer (Ktor, SwiftNIO) only needs to build the
 * schema once via [create] and dispatch `/graphql` requests to [handle].
 *
 * All authentication, decryption, replay-guard, and response encryption
 * logic lives here so the platform layer is free of business code.
 */
class MainGraphQLService private constructor(
    val schema: Schema,
) {
    /**
     * Execute a GraphQL request body against the schema, producing a JSON
     * response string. The [call] is exposed to resolvers via the KGraphQL
     * `Context` as a [GraphqlRequestContext] so they can read headers.
     */
    private suspend fun executeSchema(query: String, call: HttpCall): String = withIO {
        val request = Json.decodeFromString(GraphqlRequest.serializer(), query)
        val ctx = context {
            +GraphqlRequestContext(call)
        }
        schema.execute(
            request.query,
            request.variables?.toString(),
            ctx,
        )
    }

    /**
     * Handle a `/graphql` POST request. Supports two auth modes:
     *
     * 1. Token mode (no `Authorization` header): the body is ChaCha20-encrypted
     *    with the cached session token. The decrypted payload is validated by
     *    [ReplayGuard] before execution.
     * 2. Bearer mode (`Authorization: Bearer <token>`): the body is plain JSON
     *    and the bearer token must match a persisted custom session.
     *
     * On success the response is encrypted (token mode) or returned as plain
     * JSON (bearer mode). On failure a standard HTTP status is returned.
     */
    suspend fun handle(call: HttpCall) {
        if (!TempData.webEnabled.value) {
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return
        }

        val clientId = call.header("c-id") ?: ""
        if (clientId.isEmpty()) {
            call.respondNoBody(HttpStatus.UNAUTHORIZED)
            return
        }

        val authStr = call.header("authorization")?.split(" ")
        if (authStr.isNullOrEmpty()) {
            // Token mode — decrypt request body.
            val token = HttpServerManager.tokenCache[clientId]
            if (token == null) {
                call.respondNoBody(HttpStatus.UNAUTHORIZED)
                return
            }

            val decryptedBytes = chaCha20Decrypt(token, call.receiveBody())
            val decryptedStr = decryptedBytes?.decodeToString() ?: ""
            if (decryptedStr.isEmpty()) {
                call.respondNoBody(HttpStatus.UNAUTHORIZED)
                return
            }

            val parsed = ReplayGuard.parse(decryptedStr)
            if (parsed == null) {
                call.respondNoBody(HttpStatus.BAD_REQUEST)
                return
            }
            val err = ReplayGuard.validate(clientId, parsed)
            if (err != null) {
                call.respondNoBody(HttpStatus.BAD_REQUEST)
                return
            }

            HttpServerManager.clientRequestTs[clientId] = TimeHelper.nowMillis()
            sendEvent(WebRequestReceivedEvent())
            val result = executeSchema(parsed.body, call)
            call.respond(
                chaCha20Encrypt(token, result),
                contentType = "application/octet-stream",
            )
        } else {
            // Bearer mode — plain JSON request.
            val bearerToken = authStr.getOrNull(1) ?: ""
            val session = SessionList.getByClientIdAsync(clientId)
            if (bearerToken.isEmpty() ||
                session == null ||
                session.type != DSession.TYPE_CUSTOM ||
                session.token != bearerToken
            ) {
                call.respondNoBody(HttpStatus.UNAUTHORIZED)
                return
            }

            val requestStr = call.receiveText()
            LogCat.d("[Request] $requestStr")
            HttpServerManager.clientRequestTs[clientId] = TimeHelper.nowMillis()
            sendEvent(WebRequestReceivedEvent())
            val result = executeSchema(requestStr, call)
            call.respondText(result, contentType = "application/json")
        }
    }

    /**
     * Serialise a [GraphQLError] and, when possible, deliver it back to the
     * caller through the same encryption/bearer channel used by [handle].
     *
     * @return `true` when the error response was sent, `false` when the
     * caller's auth context could not be determined and the caller should
     * receive a generic 401 instead.
     */
    suspend fun handleError(error: GraphQLError, call: HttpCall): Boolean {
        val clientId = call.header("c-id") ?: ""
        val type = call.header("c-type") ?: "" // peer
        val channelId = call.header("c-cid") ?: "" // chat channel id
        val authStr = call.header("authorization")?.split(" ")
        return if (authStr.isNullOrEmpty()) {
            val token = if (channelId.isNotEmpty()) {
                ChannelCacher.getKeyBytes(channelId)
            } else if (type == "peer") {
                PeerCacher.getKeyBytes(channelId)
            } else {
                HttpServerManager.tokenCache[clientId]
            }
            if (token != null) {
                call.respond(
                    chaCha20Encrypt(token, error.serialize()),
                    contentType = "application/octet-stream",
                )
                true
            } else {
                false
            }
        } else {
            call.respondText(error.serialize(), contentType = "application/json")
            true
        }
    }

    companion object {
        /**
         * Build the [MainGraphQLService] by assembling the full shared
         * schema via [applyMainSchema]. The resulting [Schema] is shared by
         * the Android (Ktor) and iOS (SwiftNIO) HTTP server entry points.
         */
        fun create(): MainGraphQLService {
            val schema = KGraphQL.schema {
                applyMainSchema()
            }
            return MainGraphQLService(schema)
        }
    }
}
