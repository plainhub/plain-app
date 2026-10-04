package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.kgraphql.KGraphQL
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedPeerResolvers
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedSchema
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.models.ChatItem
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.mainschemas.addPeerSchemaTypes
import kotlinx.serialization.json.*
import kotlin.reflect.typeOf

// Native BLE/route adapter; schema is built only for contract export.
class PeerGraphQLService private constructor(
    private val contractSchema: () -> Schema,
) {
    val schema: Schema by lazy(contractSchema)

    suspend fun handle(call: HttpCall) {
        val result = com.ismartcoding.plain.api.RustContentApi.postJson("chat/peer-graphql", buildJsonObject {
            put("clientId", call.header("c-id") ?: "")
            put("channelId", call.header("c-cid") ?: "")
            put("body", kotlin.io.encoding.Base64.encode(call.receiveBody()))
        }, longRunning = true).getValue("result").jsonObject
        val status = result.getValue("status").jsonPrimitive.int
        if (status == 200) call.respond(kotlin.io.encoding.Base64.decode(result.getValue("body").jsonPrimitive.content),
            contentType = "application/octet-stream")
        else call.respondNoBody(status)
    }

    companion object {
        fun create(): PeerGraphQLService {
            return PeerGraphQLService {
                KGraphQL.schema {
                    registerGeneratedSchema()
                    addPeerSchemaTypes()
                    applyPeerSchema()
                }
            }
        }

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
