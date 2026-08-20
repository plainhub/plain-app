package com.ismartcoding.plain

import com.ismartcoding.plain.httpserver.GuestGraphQLService
import com.ismartcoding.plain.httpserver.MainGraphQLService
import com.ismartcoding.plain.httpserver.PeerGraphQLService
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import kotlin.test.Test
import java.io.File

class PrintSchemaTest {

    /**
     * Dump the SDL of the GraphQL services to `apitest/` so the schema
     * split between main / peer / guest can be inspected and diffed:
     *
     * - `schema.graphqls`       — main API (authenticated web UI)
     * - `schema-peer.graphqls`  — peer chat API (shared-key encrypted)
     * - `schema-guest.graphqls` — shared-link guest API (encrypted)
     */
    @Test
    fun printGraphQLSchemas() {
        writeSchema("schema.graphqls", MainGraphQLService.create().schema)
        writeSchema("schema-peer.graphqls", PeerGraphQLService.create().schema)
        writeSchema("schema-guest.graphqls", GuestGraphQLService.create().schema)
    }

    private fun writeSchema(fileName: String, schema: Schema) {
        val outputFile = File("apitest/$fileName")
        outputFile.parentFile.mkdirs()
        val sdl = schema.printSDL()
        outputFile.writeText(sdl)
        println("\n===== GraphQL Schema SDL: $fileName =====")
        println("SDL written to: ${outputFile.absolutePath}")
        println("SDL length: ${sdl.length} chars")
        println("===== End of $fileName =====\n")
    }
}
