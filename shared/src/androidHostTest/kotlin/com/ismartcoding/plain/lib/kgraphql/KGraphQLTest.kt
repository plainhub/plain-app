package com.ismartcoding.plain.lib.kgraphql

import com.ismartcoding.plain.lib.kgraphql.context
import com.ismartcoding.plain.lib.kgraphql.generated.GeneratedSchemaRegistry
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.TypeDSL
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Unit tests covering KGraphQL core behavior.
 * These tests exercise the PUBLIC API (KGraphQL.schema + schema.execute)
 * to ensure behavior is preserved during the reflection → pure Kotlin migration.
 *
 * NOTE: Tests run on androidHostTest (JVM) where kotlin.reflect.full is available.
 * The helpers below use JVM reflection to auto-discover properties — this is
 * test-only convenience; production code uses KSP-generated descriptors that
 * work on both Android and iOS without reflection.
 */
class KGraphQLTest {

    // ========== Test data classes ==========

    data class User(val id: String, val name: String, val age: Int)
    data class Item(val id: String, val title: String, val price: Double)
    @Serializable
    data class CreateItemInput(val title: String, val price: Double)
    enum class Status { ACTIVE, INACTIVE, PENDING }

    // ========== Test helpers (JVM reflection — test-only) ==========

    /**
     * Registers [T] as a GraphQL Object type with all its public properties
     * auto-discovered via JVM reflection. Tests run on androidHostTest (JVM)
     * where kotlin.reflect.full is available, so we can use reflection here
     * without affecting iOS. Production code uses @GraphQLType + KSP instead.
     */
    private inline fun <reified T : Any> SchemaBuilder.typeWithProperties(
        noinline block: TypeDSL<T>.() -> Unit = {}
    ) {
        type<T> {
            @Suppress("UNCHECKED_CAST")
            T::class.memberProperties.forEach { prop ->
                property(prop as KProperty1<T, *>, prop.returnType)
            }
            block()
        }
    }

    /**
     * Registers [T] as a GraphQL Input type with all its public properties
     * auto-discovered via JVM reflection. Test-only convenience.
     */
    private inline fun <reified T : Any> SchemaBuilder.inputTypeWithProperties() {
        inputType<T> {
            @Suppress("UNCHECKED_CAST")
            T::class.memberProperties.forEach { prop ->
                property(prop as KProperty1<T, *>, prop.returnType)
            }
        }
    }

    private fun executeQuery(schema: Schema, query: String, variables: String? = null): String {
        return runBlocking {
            schema.execute(query, variables, context { })
        }
    }

    private fun parseData(json: String): JsonObject {
        val parsed = Json.parseToJsonElement(json).jsonObject
        assertNotNull(parsed["data"], "Response should have 'data' key: $json")
        return parsed["data"]!!.jsonObject
    }

    // ========== Query tests ==========

    @Test
    fun `simple query with no args returns string`() {
        val schema = KGraphQL.schema {
            query("hello") {
                resolver { -> "world" }
            }
        }

        val result = executeQuery(schema, "{ hello }")
        val data = parseData(result)
        assertEquals("world", data["hello"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `query with one string arg`() {
        val schema = KGraphQL.schema {
            query("greet") {
                resolver("name") { name: String -> "Hello, $name!" }
            }
        }

        val result = executeQuery(schema, "{ greet(name: \"Alice\") }")
        val data = parseData(result)
        assertEquals("Hello, Alice!", data["greet"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `query with two args`() {
        val schema = KGraphQL.schema {
            query("add") {
                resolver("a", "b") { a: Int, b: Int -> a + b }
            }
        }

        val result = executeQuery(schema, "{ add(a: 3, b: 5) }")
        val data = parseData(result)
        assertEquals(8, data["add"]?.jsonPrimitive?.intOrNull)
    }

    @Test
    fun `query returning object with auto-discovered properties`() {
        val schema = KGraphQL.schema {
            query("user") {
                resolver { -> User("u1", "Alice", 30) }
            }
            typeWithProperties<User>()
        }

        val result = executeQuery(schema, "{ user { id name age } }")
        val data = parseData(result)
        val user = data["user"]?.jsonObject
        assertNotNull(user)
        assertEquals("u1", user!!["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Alice", user["name"]?.jsonPrimitive?.contentOrNull)
        assertEquals(30, user["age"]?.jsonPrimitive?.intOrNull)
    }

    @Test
    fun `query returning list of objects`() {
        val schema = KGraphQL.schema {
            query("users") {
                resolver { ->
                    listOf(
                        User("u1", "Alice", 30),
                        User("u2", "Bob", 25)
                    )
                }
            }
            typeWithProperties<User>()
        }

        val result = executeQuery(schema, "{ users { id name } }")
        val data = parseData(result)
        val users = data["users"] as JsonArray
        assertEquals(2, users.size)
        assertEquals("u1", users[0].jsonObject["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Alice", users[0].jsonObject["name"]?.jsonPrimitive?.contentOrNull)
        assertEquals("u2", users[1].jsonObject["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Bob", users[1].jsonObject["name"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `query returning arrayList of objects`() {
        val schema = KGraphQL.schema {
            query("users") {
                resolver { ->
                    arrayListOf(
                        User("u1", "Alice", 30),
                        User("u2", "Bob", 25)
                    )
                }
            }
            typeWithProperties<User>()
        }

        val result = executeQuery(schema, "{ users { id name } }")
        val data = parseData(result)
        val users = data["users"] as JsonArray
        assertEquals(2, users.size)
        assertEquals("u1", users[0].jsonObject["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Alice", users[0].jsonObject["name"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `type with custom property resolver`() {
        val schema = KGraphQL.schema {
            query("user") {
                resolver { -> User("u1", "Alice Smith", 30) }
            }
            typeWithProperties<User> {
                property("displayName") {
                    resolver { user: User ->
                        "${user.name} (${user.age})"
                    }
                }
            }
        }

        val result = executeQuery(schema, "{ user { name displayName } }")
        val data = parseData(result)
        val user = data["user"]?.jsonObject
        assertNotNull(user)
        assertEquals("Alice Smith", user!!["name"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Alice Smith (30)", user["displayName"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `query with long return type`() {
        val schema = KGraphQL.schema {
            query("timestamp") {
                resolver { -> 1700000000000L }
            }
        }

        val result = executeQuery(schema, "{ timestamp }")
        val data = parseData(result)
        assertEquals(1700000000000L, data["timestamp"]?.jsonPrimitive?.longOrNull)
    }

    @Test
    fun `query with boolean return type`() {
        val schema = KGraphQL.schema {
            query("isActive") {
                resolver { -> true }
            }
        }

        val result = executeQuery(schema, "{ isActive }")
        val data = parseData(result)
        assertEquals(true, data["isActive"]?.jsonPrimitive?.booleanOrNull)
    }

    @Test
    fun `query with double return type`() {
        val schema = KGraphQL.schema {
            query("price") {
                resolver { -> 9.99 }
            }
        }

        val result = executeQuery(schema, "{ price }")
        val data = parseData(result)
        val priceStr = data["price"]?.jsonPrimitive?.contentOrNull
        assertNotNull(priceStr)
        assertEquals(9.99, priceStr!!.toDouble(), 0.001)
    }

    // ========== Mutation tests ==========

    @Test
    fun `mutation with args returns result`() {
        val schema = KGraphQL.schema {
            mutation("createItem") {
                resolver("title", "price") { title: String, price: Double ->
                    Item("i1", title, price)
                }
            }
            typeWithProperties<Item>()
        }

        val result = executeQuery(schema, "mutation { createItem(title: \"Widget\", price: 19.99) { id title price } }")
        val data = parseData(result)
        val item = data["createItem"]?.jsonObject
        assertNotNull(item)
        assertEquals("i1", item!!["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Widget", item["title"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `mutation returning boolean`() {
        val schema = KGraphQL.schema {
            mutation("deleteItem") {
                resolver("id") { id: String -> true }
            }
        }

        val result = executeQuery(schema, "mutation { deleteItem(id: \"i1\") }")
        val data = parseData(result)
        assertEquals(true, data["deleteItem"]?.jsonPrimitive?.booleanOrNull)
    }

    // ========== Input type tests ==========

    @Test
    fun `mutation with input type`() {
        val schema = KGraphQL.schema {
            mutation("createItem") {
                resolver("input") { input: CreateItemInput ->
                    Item("i1", input.title, input.price)
                }
            }
            inputTypeWithProperties<CreateItemInput>()
            typeWithProperties<Item>()
        }

        val query = """
            mutation CreateItem(${'$'}input: CreateItemInput!) {
                createItem(input: ${'$'}input) { id title price }
            }
        """.trimIndent()
        val variables = """{"input": {"title": "Gadget", "price": 29.99}}"""

        val result = executeQuery(schema, query, variables)
        val data = parseData(result)
        val item = data["createItem"]?.jsonObject
        assertNotNull(item)
        assertEquals("i1", item!!["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Gadget", item["title"]?.jsonPrimitive?.contentOrNull)
    }

    // ========== Enum tests ==========

    @Test
    fun `query returning enum value`() {
        val schema = KGraphQL.schema {
            query("status") {
                resolver { -> Status.ACTIVE }
            }
            enum<Status>()
        }

        val result = executeQuery(schema, "{ status }")
        val data = parseData(result)
        assertEquals("ACTIVE", data["status"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `type with enum property`() {
        data class Task(val id: String, val status: Status)

        val schema = KGraphQL.schema {
            query("task") {
                resolver { -> Task("t1", Status.PENDING) }
            }
            typeWithProperties<Task>()
            enum<Status>()
        }

        val result = executeQuery(schema, "{ task { id status } }")
        val data = parseData(result)
        val task = data["task"]?.jsonObject
        assertNotNull(task)
        assertEquals("t1", task!!["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("PENDING", task["status"]?.jsonPrimitive?.contentOrNull)
    }

    // ========== Variables tests ==========

    @Test
    fun `query with enum variable`() {
        val schema = KGraphQL.schema {
            query("statusBy") {
                resolver("status") { status: Status -> "Status: $status" }
            }
            enum<Status>()
        }

        val query = """
            query StatusQuery(${'$'}status: Status!) {
                statusBy(status: ${'$'}status)
            }
        """.trimIndent()
        val variables = """{"status": "ACTIVE"}"""

        val result = executeQuery(schema, query, variables)
        val data = parseData(result)
        assertEquals("Status: ACTIVE", data["statusBy"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `query with variables`() {
        val schema = KGraphQL.schema {
            query("greet") {
                resolver("name") { name: String -> "Hello, $name!" }
            }
        }

        val query = """
            query Greet(${'$'}name: String!) {
                greet(name: ${'$'}name)
            }
        """.trimIndent()
        val variables = """{"name": "Bob"}"""

        val result = executeQuery(schema, query, variables)
        val data = parseData(result)
        assertEquals("Hello, Bob!", data["greet"]?.jsonPrimitive?.contentOrNull)
    }

    // ========== Error handling tests ==========

    @Test
    fun `query with missing required arg throws error`() {
        val schema = KGraphQL.schema {
            query("greet") {
                resolver("name") { name: String -> "Hello, $name!" }
            }
        }

        assertFailsWith<Exception> {
            executeQuery(schema, "{ greet }")
        }
    }

    @Test
    fun `null return value for nullable type`() {
        val schema = KGraphQL.schema {
            query("maybeNull") {
                resolver { -> null as String? }
            }
        }

        val result = executeQuery(schema, "{ maybeNull }")
        val parsed = Json.parseToJsonElement(result).jsonObject
        val data = parsed["data"]!!.jsonObject
        assertNull(data["maybeNull"]?.jsonPrimitive?.contentOrNull)
    }

    // ========== Alias tests ==========

    @Test
    fun `query with alias`() {
        val schema = KGraphQL.schema {
            query("greet") {
                resolver("name") { name: String -> "Hello, $name!" }
            }
        }

        val result = executeQuery(schema, "{ a: greet(name: \"Alice\") b: greet(name: \"Bob\") }")
        val data = parseData(result)
        assertEquals("Hello, Alice!", data["a"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Hello, Bob!", data["b"]?.jsonPrimitive?.contentOrNull)
    }

    // ========== Multiple queries ==========

    @Test
    fun `multiple queries in one request`() {
        val schema = KGraphQL.schema {
            query("hello") {
                resolver { -> "world" }
            }
            query("count") {
                resolver { -> 42 }
            }
        }

        val result = executeQuery(schema, "{ hello count }")
        val data = parseData(result)
        assertEquals("world", data["hello"]?.jsonPrimitive?.contentOrNull)
        assertEquals(42, data["count"]?.jsonPrimitive?.intOrNull)
    }

    // ========== Property transformation tests ==========

    @Test
    fun `type with transformation property`() {
        data class Product(val id: String, val priceCents: Int)

        val schema = KGraphQL.schema {
            query("product") {
                resolver { -> Product("p1", 9999) }
            }
            typeWithProperties<Product> {
                transformation(Product::priceCents, "divisor") { cents: Int, divisor: Int ->
                    cents / divisor
                }
            }
        }

        val result = executeQuery(schema, "{ product { id priceCents(divisor: 100) } }")
        val data = parseData(result)
        val product = data["product"]?.jsonObject
        assertNotNull(product)
        assertEquals("p1", product!!["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals(99, product["priceCents"]?.jsonPrimitive?.intOrNull)
    }

    // ========== KSP synthesis path tests ==========

    @Test
    fun `type synthesized from KSP descriptor without explicit type declaration`() {
        // Simulates the production scenario where a type is referenced via a
        // resolver return type but NOT explicitly declared with `type<T> {}`.
        // The schema compiler must synthesize the TypeDef.Object from the KSP
        // descriptor in GeneratedSchemaRegistry and still expose its fields.
        data class SynthType(val id: String, val label: String)

        // Manually register a KSP descriptor (production code gets this from KSP).
        GeneratedSchemaRegistry.registerType(
            com.ismartcoding.plain.lib.kgraphql.generated.TypeDescriptor(
                kClass = SynthType::class,
                name = "SynthType",
                isInterface = false,
                fields = listOf(
                    com.ismartcoding.plain.lib.kgraphql.generated.FieldDescriptor(
                        name = "id",
                        kProperty = SynthType::id,
                        returnType = kotlin.reflect.typeOf<String>()
                    ),
                    com.ismartcoding.plain.lib.kgraphql.generated.FieldDescriptor(
                        name = "label",
                        kProperty = SynthType::label,
                        returnType = kotlin.reflect.typeOf<String>()
                    )
                )
            )
        )

        val schema = KGraphQL.schema {
            // NOTE: no `type<SynthType> {}` — must be synthesized from the registry.
            query("synth") {
                resolver { -> SynthType("s1", "hello") }
            }
        }

        val result = executeQuery(schema, "{ synth { id label } }")
        val data = parseData(result)
        val synth = data["synth"]?.jsonObject
        assertNotNull(synth)
        assertEquals("s1", synth!!["id"]?.jsonPrimitive?.contentOrNull)
        assertEquals("hello", synth["label"]?.jsonPrimitive?.contentOrNull)
    }
}
