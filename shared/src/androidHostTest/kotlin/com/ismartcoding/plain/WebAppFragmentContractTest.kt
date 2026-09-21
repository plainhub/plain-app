package com.ismartcoding.plain

import com.ismartcoding.plain.httpserver.MainGraphQLService
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Check the shipped browser code, not just a sibling frontend source checkout. */
class WebAppFragmentContractTest {
    @Test
    fun bundledAppFragmentUsesCurrentServerFields() {
        val schema = MainGraphQLService.create().schema.printSDL()
        val appType = Regex("type App\\s*\\{([\\s\\S]*?)\\}").find(schema)
            ?: error("App type missing from runtime schema")
        val fields = Regex("(?m)^\\s*(\\w+)\\s*:").findAll(appType.groupValues[1])
            .map { it.groupValues[1] }.toSet()
        val assets = File("../app/src/main/resources/web/assets")
        assertTrue(assets.isDirectory, "Bundled web assets are missing")
        val fragments = assets.walkTopDown().filter { it.isFile && it.extension == "js" }
            .flatMap { file ->
                Regex("fragment AppFragment on App\\s*\\{([^}]*)\\}")
                    .findAll(file.readText()).map { file.name to it.groupValues[1] }
            }.toList()
        assertTrue(fragments.isNotEmpty(), "No AppFragment found in the bundled frontend; review the bundle contract check")
        for ((file, fragment) in fragments) {
            val requested = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b").findAll(fragment)
                .map { it.value }.toSet()
            val missing = requested - fields - "__typename"
            assertTrue(missing.isEmpty(),
                "$file requests removed App fields $missing. Rebuild/sync plain-desktop's web dist into app/src/main/resources/web before packaging.")
        }
    }
}
