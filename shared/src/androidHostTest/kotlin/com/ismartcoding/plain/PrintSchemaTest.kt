package com.ismartcoding.plain

import com.ismartcoding.plain.httpserver.MainGraphQLService
import kotlin.test.Test
import java.io.File

class PrintSchemaTest {

    @Test
    fun printGraphQLSchema() {
        val schema = MainGraphQLService.create().schema
        val sdl = schema.printSDL()
        val outputFile = File("apitest/schema.graphqls")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(sdl)
        println("\n===== GraphQL Schema SDL =====")
        println("SDL written to: ${outputFile.absolutePath}")
        println("SDL length: ${sdl.length} chars")
        println("===== End of Schema =====\n")
    }
}