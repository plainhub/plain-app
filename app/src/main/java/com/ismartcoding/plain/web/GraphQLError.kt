package com.ismartcoding.plain.web

import com.apurebase.kgraphql.GraphQLError
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
                    put(
                        "locations",
                        buildJsonArray {
                            locations?.forEach {
                                addJsonObject {
                                    put("line", it.line)
                                    put("column", it.column)
                                }
                            }
                        },
                    )
                    put(
                        "path",
                        buildJsonArray {
                            // TODO: Build this path. https://spec.graphql.org/June2018/#example-90475
                        },
                    )
                }
            },
        )
    }.toString()
