package com.ismartcoding.plain.api.graphql

import com.ismartcoding.plain.features.files.FileTaskOp
import com.ismartcoding.plain.features.nearby.PairingMode
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ImageEditorProject
import com.ismartcoding.plain.models.PairPeerInput
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.platform.system.DataType
import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import java.lang.Boolean as JavaBoolean

class GraphqlVariableContractTest {
    @Test
    fun variableDeclarationsMatchTheSharedSchema() {
        val schema = readMainSchema().replace(Regex("\"\"\"[\\s\\S]*?\"\"\""), "")
        val types = Regex("""(?:extend\s+)?type\s+(Query|Mutation)\s*\{([\s\S]*?)\n}""")
            .findAll(schema).groupBy { it.groupValues[1].lowercase() }
            .mapValues { (_, blocks) -> blocks.flatMap { block ->
                Regex("""(?m)^\s*(\w+)\s*\(([^)]*)\)\s*:""").findAll(block.groupValues[2]).map { field ->
                    field.groupValues[1] to Regex("""(\w+)\s*:\s*([\w!\[\]]+)""").findAll(field.groupValues[2])
                        .associate { it.groupValues[1] to it.groupValues[2] }
                }.toList()
            }.toMap() }
        var checked = 0
        for (owner in listOf(Query, Mutation)) {
            for (method in owner.javaClass.declaredMethods.filter { it.returnType == GraphqlRequest::class.java && !it.isSynthetic }) {
                if (!method.parameterTypes.all(::supported)) continue
                val arguments = method.parameterTypes.mapIndexed { index, type -> argument(method, index, type) }.toTypedArray()
                val request = method.invoke(owner, *arguments) as GraphqlRequest<*>
                val definitions = Regex("""\$(\w+)\s*:\s*([\w!\[\]]+)""").findAll(request.operation.query.substringBefore('{'))
                    .associate { it.groupValues[1] to it.groupValues[2] }
                assertTrue(definitions.keys.containsAll(request.operation.variables.keys), method.name)
                definitions.filterValues { it.endsWith('!') }.keys.forEach { name ->
                    assertTrue(name in request.operation.variables, "${method.name}: missing required variable $name")
                }
                val root = types.getValue(request.operation.query.substringBefore('(').substringBefore(' '))
                for (call in Regex("""\b(\w+)\(([^()]*)\)""").findAll(request.operation.query.substringAfter('{'))) {
                    val expected = root[call.groupValues[1]] ?: continue
                    for (argument in Regex("""(\w+)\s*:\s*\$(\w+)""").findAll(call.groupValues[2])) {
                        val actualType = definitions.getValue(argument.groupValues[2])
                        assertCompatible(expected.getValue(argument.groupValues[1]), actualType, method.name)
                    }
                }
                checked++
            }
        }
        assertTrue(checked >= 150, "Only $checked request builders checked")
    }

    private fun supported(type: Class<*>): Boolean = type in setOf(
        String::class.java, Int::class.javaPrimitiveType, Long::class.javaPrimitiveType,
        Float::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, JavaBoolean::class.java,
        Instant::class.java, ImageEditorProject::class.java, PairPeerInput::class.java,
    ) || type.isEnum || Collection::class.java.isAssignableFrom(type)

    private fun argument(method: Method, index: Int, type: Class<*>): Any = when {
        type == String::class.java -> when {
            method.name == "selectedMediaPaths" && index == 0 -> "images"
            method.name == "fileWrite" && index == 0 -> "writeText"
            method.name in setOf("sharedDownloadControl", "chatDownloadControl") && index == 0 -> "retry"
            else -> "fixture"
        }
        type == Int::class.javaPrimitiveType -> 1
        type == Long::class.javaPrimitiveType -> 1L
        type == Float::class.javaPrimitiveType -> 1f
        type == Boolean::class.javaPrimitiveType || type == JavaBoolean::class.java -> false
        type == Instant::class.java -> Instant.parse("2026-01-01T00:00:00Z")
        type == ImageEditorProject::class.java -> ImageEditorProject("fixture")
        type == PairPeerInput::class.java -> PairPeerInput(PairingMode.CHAT)
        type == DataType::class.java -> DataType.IMAGE
        type.isEnum -> type.enumConstants.first()
        method.name == "createFileTask" -> listOf(FileTaskOp("source", "target"))
        else -> listOf("fixture")
    }

    private fun assertCompatible(expected: String, actual: String, context: String) {
        if (expected.endsWith('!')) assertTrue(actual.endsWith('!'), "$context: $actual cannot supply $expected")
        val expectedValue = expected.removeSuffix("!")
        val actualValue = actual.removeSuffix("!")
        if (expectedValue.startsWith('[') && actualValue.startsWith('[')) {
            assertCompatible(expectedValue.substring(1, expectedValue.lastIndex), actualValue.substring(1, actualValue.lastIndex), context)
        } else {
            assertEquals(expectedValue, actualValue, context)
        }
    }
}
