package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpStatus
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.platform.chaCha20Encrypt

/**
 * Deliver a [GraphQLError] back to the caller through the same
 * encryption/bearer channel the request arrived on.
 *
 * Only the Kotlin-side routes still need this: the public `/graphql` and the
 * peer/guest GraphQL endpoints run in Rust and render their own errors. It
 * remains for the bridge-served routes and their GraphQL-adjacent failures.
 *
 * @return `true` when the error response was sent, `false` when the caller's
 * auth context could not be determined and the caller should receive a
 * generic 401 instead.
 */
suspend fun handleGraphQLError(error: GraphQLError, call: HttpCall): Boolean {
    val clientId = call.header("c-id") ?: ""
    val channelId = call.header("c-cid") ?: "" // chat channel id
    val authStr = call.header("authorization")?.split(" ")
    return if (authStr.isNullOrEmpty()) {
        // Token mode — pick the decryption key the same way the route
        // handlers do: channel key for channel chat, peer shared key for
        // peer-to-peer chat, session token for regular web clients.
        val token = if (channelId.isNotEmpty()) {
            ChannelCacher.getKeyBytes(channelId)
        } else {
            PeerCacher.getKeyBytes(clientId) ?: HttpServerManager.tokenCache.get(clientId)
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