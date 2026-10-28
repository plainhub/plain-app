package com.ismartcoding.plain.features.sessions

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.ui.sessions.VSession
import kotlin.time.Instant

object RustSessionStore {
    suspend fun list(): List<VSession> = GraphqlApi.execute(Query.sessions()).map {
        VSession(
            it.clientId,
            it.name,
            it.type,
            it.token,
            it.clientIp,
            it.osName,
            it.osVersion,
            it.browserName,
            it.browserVersion,
            Instant.parse(it.createdAt),
            Instant.parse(it.updatedAt),
            it.lastSeenAt?.let(Instant::parse),
        )
    }

    suspend fun create(name: String): CreatedSession = RustNativeApi.createSession(name)

    suspend fun rename(clientId: String, name: String): Boolean =
        GraphqlApi.execute(Mutation.renameSession(clientId, name))

    suspend fun delete(clientId: String) {
        GraphqlApi.execute(Mutation.revokeSession(clientId))
    }
}
