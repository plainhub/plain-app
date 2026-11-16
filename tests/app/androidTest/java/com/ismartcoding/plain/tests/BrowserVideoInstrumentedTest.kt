package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.media.MediaClient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.network.TestHttpClientSpec
import com.ismartcoding.plain.network.createTestHttpClient
import com.ismartcoding.plain.network.get
import com.ismartcoding.plain.rust.RustRuntime
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.native.nativeCall
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class BrowserVideoInstrumentedTest {
    @Serializable
    private data class Access(val clientId: String, val urlToken: String, val httpPort: Int)

    @Test
    fun publicResourceTranscodesHevcOrReportsUnsupportedMedia() = runBlocking {
        val source = File(appContext.cacheDir, "hevc_probe.mp4")
        assumeTrue("HEVC fixture has not been placed in the app cache", source.isFile && source.canRead())
        val service = Prefs.backgroundServiceEnabled.value
        val desktop = Prefs.remoteAccess.value
        val output = File(appContext.cacheDir, "browser-video-${UUID.randomUUID()}.mp4")
        try {
            Prefs.backgroundServiceEnabled.value = true
            Prefs.remoteAccess.value = true
            withIO { RustRuntime.start() }
            val access = GraphqlApi.execute(GraphqlRequest<Access>("query { app { clientId urlToken httpPort } }", field = "app"))
            assertTrue(access.httpPort > 0)
            suspend fun resource(file: File, action: String): String {
                val payload = JsonHelper.jsonEncode(mapOf("clientId" to access.clientId, "path" to file.absolutePath))
                val id = nativeCall<String> { RustNativeBindings.encryptResourceReference(access.urlToken, payload) }
                return "http://127.0.0.1:${access.httpPort}/fs?id=${URLEncoder.encode(id, "UTF-8")}&action=$action"
            }
            createTestHttpClient(TestHttpClientSpec.Local()).use { client ->
                client.get(resource(source, "probe")).use { response ->
                    assertEquals(200, response.status.value)
                    val codec = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("codec").jsonPrimitive.content
                    assertTrue(codec == "hvc1" || codec == "hev1")
                }
                client.get(resource(source, "transcode")).use { response ->
                    if (response.status.value == 415) {
                        assertEquals("video transcoding is not available for this file", response.bodyAsText())
                        assertFalse(output.exists())
                        return@runBlocking
                    }
                    assertEquals(200, response.status.value)
                    output.writeBytes(response.bodyAsBytes())
                }
                assertTrue(output.length() > 0)
                client.get(resource(output, "probe")).use { response ->
                    assertEquals(200, response.status.value)
                    assertEquals("avc1", Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("codec").jsonPrimitive.content)
                }
                val sourceDuration = MediaClient.durationMs(source.absolutePath)
                val outputDuration = MediaClient.durationMs(output.absolutePath)
                assertTrue(sourceDuration > 0)
                assertTrue(abs(sourceDuration - outputDuration) <= 2000)
            }
        } finally {
            output.delete()
            Prefs.remoteAccess.value = desktop
            Prefs.backgroundServiceEnabled.value = service
        }
    }
}
