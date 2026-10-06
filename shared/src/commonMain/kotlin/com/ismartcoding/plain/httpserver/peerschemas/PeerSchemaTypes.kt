package com.ismartcoding.plain.httpserver.peerschemas

import com.ismartcoding.plain.enums.ChannelSystemMessageType
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.enums.PeerStatus
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import kotlin.time.Instant

/**
 * Types used by the peer-chat schema (`/peer_graphql`): the `ChatItem` result
 * references [ID], [Instant] and [ChatStatus]; the `channelSystemMessage`
 * mutation takes a [ChannelSystemMessageType].
 *
 * Lives beside [PeerGraphQLService] rather than with the main schema, which
 * Rust now serves: only the peer/guest schemas are still built here.
 */
fun SchemaBuilder.addPeerSchemaTypes() {
    enum<PeerStatus>()
    enum<ChatStatus>()
    enum<ChannelSystemMessageType>()
    stringScalar<ID> {
        // Scalar names must be compile-time fixed: the DSL defaults to
        // KClass.simpleName, which R8 renames in release builds (e.g. "p94").
        name = "ID"
        deserialize = { it: String -> ID(it) }
        serialize = { it: ID -> it.toString() }
    }
    stringScalar<Instant> {
        name = "Instant"
        description = "ISO-8601 / RFC 3339 UTC timestamp string, e.g. 2026-09-20T12:34:56.789Z"
        deserialize = { value: String -> Instant.parse(value) }
        serialize = Instant::toString
    }
}