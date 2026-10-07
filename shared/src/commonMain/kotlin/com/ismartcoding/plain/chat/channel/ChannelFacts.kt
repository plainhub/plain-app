package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.db.DChatChannel
import kotlinx.serialization.json.*

/** The `ChatChannel` contract row. `members` is a JSON array string in the
 * store, so it is projected into the contract's list shape here rather than
 * handed to Rust as an opaque blob. */
internal fun channelFacts(channel: DChatChannel): JsonObject {
    val members = channel.members.map { member ->
        buildJsonObject {
            put("peerId", JsonPrimitive(member.peerId))
            put("status", JsonPrimitive(member.status.name))
        }
    }
    return buildJsonObject {
        put("id", JsonPrimitive(channel.id)); put("ownerId", JsonPrimitive(channel.ownerId))
        put("name", JsonPrimitive(channel.name)); put("members", JsonArray(members))
        put("version", JsonPrimitive(channel.version)); put("status", JsonPrimitive(channel.status.name))
        put("createdAt", JsonPrimitive(channel.createdAt.toString()))
        put("updatedAt", JsonPrimitive(channel.updatedAt.toString()))
    }
}
