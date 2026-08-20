package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.lib.kgraphql.GraphqlRequest
import com.ismartcoding.plain.lib.kgraphql.KGraphQL
import com.ismartcoding.plain.lib.kgraphql.context
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedGuestResolvers
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedSchema
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.httpserver.http.GraphqlRequestContext
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpStatus
import kotlinx.serialization.json.Json

/**
 * GraphQL entry point for shared-file links (`/guest_graphql`).
 *
 * The schema is intentionally small — it exposes only the `sharedInfo` query
 * (see [com.ismartcoding.plain.httpserver.guestschemas]). Requests reuse the
 * token-mode flow via [TokenGraphQLHandler], but the key is the share's derived
 * `shared_token` (HMAC(masterSecret, shared_id)) instead of the session token.
 *
 * Access is gated on `serviceEnabled` plus an active (not revoked/expired)
 * share for the `c-id` header. `desktopAccessEnabled` is NOT required — the
 * share link is meant to work as a standalone page.
 */
class GuestGraphQLService private constructor(
    val schema: Schema,
) {
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
     * Handle a `/guest_graphql` POST. The body is ChaCha20-encrypted with the
     * derived `shared_token`; decryption, replay-guard, and response encryption
     * are delegated to [TokenGraphQLHandler].
     */
    suspend fun handle(call: HttpCall) {
        if (!TempData.serviceEnabled.value) {
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return
        }

        val sharedId = call.header("c-id") ?: ""
        if (sharedId.isEmpty()) {
            call.respondNoBody(HttpStatus.UNAUTHORIZED)
            return
        }

        // Gate on an active share for this id. The share snapshot + derived
        // token are cached per shared_id; a negative (unknown) entry rejects
        // fast without hitting the DB, and `isActive` is still evaluated from
        // the snapshot's `expiresAt` at request time.
        val auth = ShareManager.authCache.get(sharedId)
        if (auth == null || !auth.share.isActive) {
            LogCat.w("[GuestGraphQL] reject inactive share id=$sharedId")
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return
        }

        TokenGraphQLHandler.handle(sharedId, auth.token, call) { query, c ->
            executeSchema(query, c)
        }
    }

    companion object {
        fun create(): GuestGraphQLService {
            val schema = KGraphQL.schema {
                registerGeneratedSchema()
                registerGeneratedGuestResolvers()
            }
            return GuestGraphQLService(schema)
        }
    }
}