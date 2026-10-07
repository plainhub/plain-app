package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun GraphQLError.serialize(): String =
    buildJsonObject {
        put(
            "errors",
            buildJsonArray {
                addJsonObject {
                    put("message", message)
                    // Bridge routes have no request AST to resolve a position
                    // against, so both stay empty rather than being omitted.
                    put("locations", buildJsonArray { })
                    put("path", buildJsonArray { })
                }
            },
        )
    }.toString()