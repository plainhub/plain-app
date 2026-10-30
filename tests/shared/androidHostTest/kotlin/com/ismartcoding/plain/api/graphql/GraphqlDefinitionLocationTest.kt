package com.ismartcoding.plain.api.graphql

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GraphqlDefinitionLocationTest {
    @Test
    fun featureCallersDoNotDefineGraphqlExpressionsOrVariableNames() {
        val src = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "shared/src") }.first { it.isDirectory }
        val forbidden = Regex("""\b(?:gql|gqlIds)\(|::gql\b|\bFragments\.|GraphqlApi\.(?:query|mutate|graphql)\(\s*"|(?:Query|Mutation)\.\w+\([^\n]*,\s*mapOf\(""")
        val violations = listOf("commonMain", "androidMain", "iosMain").flatMap { sourceSet ->
            val root = File(src, "$sourceSet/kotlin/com/ismartcoding/plain")
            root.walkTopDown().filter { it.extension == "kt" }
                .filterNot { it.relativeTo(root).invariantSeparatorsPath.startsWith("graphql/") }
                .flatMap { file -> file.readLines().mapIndexedNotNull { index, line ->
                    if (forbidden.containsMatchIn(line)) "${file.relativeTo(src)}:${index + 1}" else null
                } }.toList()
        }
        assertEquals(emptyList(), violations)
        for (name in listOf("Query", "Mutation", "Subscription")) {
            val definitions = File(src, "commonMain/kotlin/com/ismartcoding/plain/graphql/$name.kt").readText()
            assertFalse("JsonPrimitive" in definitions, name)
            assertFalse("JsonHelper" in definitions, name)
            assertFalse(Regex("""\b(?:jsonEncode|gql|gqlIds)\(""").containsMatchIn(definitions), name)
        }
    }
}
