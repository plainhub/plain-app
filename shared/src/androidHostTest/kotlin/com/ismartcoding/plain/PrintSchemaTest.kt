package com.ismartcoding.plain

import com.ismartcoding.plain.httpserver.GuestGraphQLService
import com.ismartcoding.plain.httpserver.PeerGraphQLService
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import kotlin.test.Test
import java.io.File

/**
 * Dump the SDL of the GraphQL services still built in Kotlin to `apitest/` so
 * the peer / guest schemas can be inspected and diffed:
 *
 * - `schema-peer.graphqls`  — peer chat API (shared-key encrypted)
 * - `schema-guest.graphqls` — shared-link guest API (encrypted)
 *
 * The main API's SDL is no longer printed here: Rust owns that schema and
 * prints `plain-rs/testdata/public-schema.graphqls` itself (see the
 * `public_` tests in the plain-rs crate).
 */
class PrintSchemaTest {

    @Test
    fun printGraphQLSchemas() {
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