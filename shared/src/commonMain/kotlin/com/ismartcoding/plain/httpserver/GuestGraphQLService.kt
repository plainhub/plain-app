package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.lib.kgraphql.KGraphQL
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedGuestResolvers
import com.ismartcoding.plain.lib.kgraphql.generated.registerGeneratedSchema
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.*

class GuestGraphQLService private constructor(val schema: Schema) {
    suspend fun handle(call: HttpCall) {
        val result = RustContentApi.postJson("http/guest", buildJsonObject {
            put("clientId", call.header("c-id").orEmpty())
            put("body", Base64.Default.encode(call.receiveBody()))
        })
        call.responseStatus(result.getValue("status").jsonPrimitive.int)
        call.respond(Base64.Default.decode(result.getValue("body").jsonPrimitive.content), contentType = "application/octet-stream")
    }

    companion object {
        fun create(): GuestGraphQLService = GuestGraphQLService(KGraphQL.schema {
            registerGeneratedSchema()
            registerGeneratedGuestResolvers()
        })
    }
}
