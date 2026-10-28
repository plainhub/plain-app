package com.ismartcoding.plain.tests

import com.ismartcoding.plain.rust.graphql.GraphqlApi

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.sessions.RustSessionStore
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.network.TestHttpClientSpec
import com.ismartcoding.plain.network.createTestHttpClient
import com.ismartcoding.plain.network.postText
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainGraphqlRustHttpTest {
    @Test
    fun jniAndAuthenticatedHttpUseOneSchemaAndKeepCredentialReadsLocal() = runBlocking {
        val name = "main-contract-${UUID.randomUUID()}"
        val oldService = Prefs.backgroundServiceEnabled.value
        val oldAccess = Prefs.desktopAccess.value
        var id: String? = null
        val httpClient = createTestHttpClient(TestHttpClientSpec.Local())
        try {
            Prefs.backgroundServiceEnabled.value = true
            Prefs.desktopAccess.value = true
            val created = RustSessionStore.create(name)
            val clientId = created.clientId
            id = clientId
            val token = created.token
            assertTrue(token.isNotEmpty())
            val app = GraphqlApi.execute(GraphqlRequest<JsonObject>("query { app { httpPort urlToken capabilities } }")).getValue("app").jsonObject
            assertEquals(getDeviceCapabilities().map { it.name }.toSet(), app.getValue("capabilities").jsonArray.map { it.jsonPrimitive.content }.toSet())
            val baseUrl = "http://127.0.0.1:${app.getValue("httpPort").jsonPrimitive.int}"
            suspend fun httpGraphql(document: String, variables: Map<String, JsonElement> = emptyMap()): JsonObject {
                val body = JsonHelper.jsonEncode(HttpGraphqlRequest(document, variables))
                val headers = mapOf("c-id" to clientId, "Authorization" to "Bearer $token")
                return httpClient.postText("$baseUrl/graphql", body, "application/json", headers).use { response ->
                    check(response.isOk()) { "Rust API returned HTTP ${response.status}" }
                    val envelope = JsonHelper.jsonDecode<JsonObject>(response.bodyAsText())
                    val errors = envelope["errors"]?.takeUnless { it is JsonNull }?.jsonArray
                    check(errors.isNullOrEmpty()) { errors?.joinToString { it.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "Rust API error" } ?: "Rust API error" }
                    requireNotNull(envelope["data"]?.takeUnless { it is JsonNull }?.jsonObject) { "Rust API response missing data" }
                }
            }
            suspend fun httpQuery(selection: String): JsonObject = httpGraphql("query { $selection }")
            val schema = "__schema { queryType { name } mutationType { name } }"
            assertEquals(GraphqlApi.execute(GraphqlRequest<JsonObject>("query { $schema }")), httpQuery(schema))
            assertEquals(app.getValue("urlToken"), httpQuery("app { urlToken }").getValue("app").jsonObject.getValue("urlToken"))
            Prefs.load()
            assertEquals(app.getValue("urlToken").jsonPrimitive.content, Prefs.urlToken.value)
            for (selection in listOf("systemPrefs", "systemSettings { password }", "userSettings { service }")) {
                for (useHttp in listOf(false, true)) {
                    val failure = runCatching { if (useHttp) httpQuery(selection) else GraphqlApi.execute(GraphqlRequest<JsonObject>("query { $selection }")) }.exceptionOrNull()
                    assertNotNull(failure)
                    assertTrue(failure.toString().contains("Unknown field"))
                }
            }
            assertTrue(runCatching { httpQuery("sessions { token }") }.isFailure)
            assertTrue(httpQuery("sessions { clientId }").getValue("sessions").jsonArray.any { it.jsonObject.getValue("clientId").jsonPrimitive.content == clientId })
            assertTrue(httpGraphql("mutation(\$id:String!,\$name:String!){renameSession(clientId:\$id,name:\$name)}", mapOf("id" to JsonPrimitive(clientId), "name" to JsonPrimitive("$name renamed"))).getValue("renameSession").jsonPrimitive.boolean)
            assertEquals("$name renamed", RustSessionStore.list().single { it.clientId == clientId }.name)
        } finally {
            httpClient.close()
            id?.let { RustSessionStore.delete(it) }
            Prefs.backgroundServiceEnabled.value = oldService
            Prefs.desktopAccess.value = oldAccess
        }
    }

    @Test
    fun selectedDeviceFieldsReadActualSdkFactsThroughTheMainNativeExecutor() = runBlocking {
        val data = GraphqlApi.execute(GraphqlRequest<JsonObject>("query { deviceInfo { name model appVersion appBuildNumber kernelVersion totalMemory display { width height density } } }")).getValue("deviceInfo").jsonObject
        assertTrue(data.getValue("model").jsonPrimitive.content.isNotEmpty())
        assertTrue(data.getValue("appVersion").jsonPrimitive.content.isNotEmpty())
        assertTrue(data.getValue("kernelVersion").jsonPrimitive.content.isNotEmpty())
        assertTrue(data.getValue("totalMemory").jsonPrimitive.long > 0)
        val display = data.getValue("display").jsonObject
        assertTrue(display.getValue("width").jsonPrimitive.int > 0)
        assertTrue(display.getValue("height").jsonPrimitive.int > 0)
        assertTrue(display.getValue("density").jsonPrimitive.double > 0)
    }
}

@Serializable
private data class HttpGraphqlRequest(val query: String, val variables: Map<String, JsonElement>)
