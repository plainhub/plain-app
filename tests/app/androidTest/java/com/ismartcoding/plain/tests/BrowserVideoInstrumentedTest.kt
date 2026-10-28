package com.ismartcoding.plain.tests

import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.rust.RustRuntime

import com.ismartcoding.plain.rust.graphql.GraphqlApi

import com.ismartcoding.plain.features.media.MediaClient

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.network.*
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class BrowserVideoInstrumentedTest {
    @Test
    fun publicResourceTranscodesHevcWithTheRustOwnedPipeline() = runBlocking {
        val source = File("/data/local/tmp/hevc_probe.mp4")
        assumeTrue("HEVC fixture is missing", source.isFile)
        val capabilities = GraphqlApi.execute(GraphqlRequest<JsonObject>("query { app { capabilities } }")).getValue("app").jsonObject
            .getValue("capabilities").jsonArray.map { it.jsonPrimitive.content }
        assumeTrue("HEVC transcoding is unavailable", "VIDEO_TRANSCODE" in capabilities)
        val service = Prefs.backgroundServiceEnabled.value
        val desktop = Prefs.desktopAccess.value
        val output = File(appContext.cacheDir, "browser-video-${UUID.randomUUID()}.mp4")
        try {
            Prefs.backgroundServiceEnabled.value = true
            Prefs.desktopAccess.value = true
            withIO { RustRuntime.start() }
            val base = "http://127.0.0.1:${Prefs.httpPort.value}"
            fun resource(file: File) = "$base/fs?id=${URLEncoder.encode(ResourceCrypto.encrypt(file.absolutePath), "UTF-8")}"
            createTestHttpClient(TestHttpClientSpec.Local()).use { client ->
                client.get("${resource(source)}&probe=1").use { response ->
                    assertEquals(200, response.status.value)
                    val codec = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("codec").jsonPrimitive.content
                    assertTrue(codec == "hvc1" || codec == "hev1")
                }
                client.get("${resource(source)}&tr=1").use { response ->
                    assertEquals(200, response.status.value)
                    output.writeBytes(response.bodyAsBytes())
                }
                assertTrue(output.length() > 0)
                client.get("${resource(output)}&probe=1").use { response ->
                    assertEquals(200, response.status.value)
                    assertEquals("avc1", Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("codec").jsonPrimitive.content)
                }
                val sourceDuration = MediaClient.durationMs(source.absolutePath)
                val outputDuration = MediaClient.durationMs(output.absolutePath)
                assertTrue(sourceDuration > 0)
                assertTrue(abs(sourceDuration-outputDuration) <= 2000)
            }
        } finally {
            output.delete()
            Prefs.desktopAccess.value = desktop
            Prefs.backgroundServiceEnabled.value = service
        }
    }
}
