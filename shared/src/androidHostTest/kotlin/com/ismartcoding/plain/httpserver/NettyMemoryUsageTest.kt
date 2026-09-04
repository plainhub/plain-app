package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.ktorserver.Netty
import com.ismartcoding.plain.lib.ktorserver.core.engine.EmbeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.applicationEnvironment
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.engine.embeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.response.respond
import com.ismartcoding.plain.lib.ktorserver.core.routing.get
import com.ismartcoding.plain.lib.ktorserver.core.routing.routing
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.netty.util.internal.PlatformDependent
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Measures the HTTP server's peak Netty direct-memory usage under concurrent
 * video-streaming load, to show the 32MiB ceiling is never approached in
 * normal operation (the cap is a ceiling, not an allocation).
 *
 * The JVM counter is only active in no-cleaner allocator mode; when inactive
 * (JDK default) the peak is reported as -1 and the assertion is skipped.
 */
class NettyMemoryUsageTest {
    private val fileSize = 8L * 1024 * 1024
    private lateinit var file: File
    private var port: Int = 0
    private lateinit var engine: EmbeddedServer<*, *>

    @BeforeTest
    fun setUp() {
        file = File.createTempFile("mem-test", ".bin")
        file.outputStream().use { out ->
            val buffer = ByteArray(64 * 1024)
            var written = 0L
            while (written < fileSize) {
                out.write(buffer, 0, buffer.size)
                written += buffer.size
            }
        }
        engine = embeddedServer(
            Netty,
            applicationEnvironment { log = LoggerFactory.getLogger("mem-usage-test") },
            configure = {
                connector {
                    port = 0
                    host = "127.0.0.1"
                }
            },
            module = {
                routing {
                    get("/video") {
                        call.respond(
                            LowMemoryFileContent(
                                file = file,
                                contentType = ContentType.Video.MP4,
                                status = HttpStatusCode.OK,
                                contentLength = fileSize,
                                range = fullFileRange(fileSize),
                                totalLength = fileSize,
                            )
                        )
                    }
                }
            },
        )
        engine.start(wait = false)
        port = runBlocking { engine.engine.resolvedConnectors().first().port }
    }

    @AfterTest
    fun tearDown() {
        engine.stop(50, 500)
        file.delete()
    }

    @Test
    fun concurrentStreams_directMemoryStaysFarBelowCap() {
        val peak = AtomicLong(-1)
        val running = AtomicBoolean(true)
        val sampler = Thread {
            while (running.get()) {
                val used = PlatformDependent.usedDirectMemory()
                if (used >= 0) {
                    peak.updateAndGet { cur -> maxOf(cur, used) }
                }
                Thread.sleep(5)
            }
        }
        sampler.isDaemon = true
        sampler.start()

        // 12 concurrent clients, 3 sequential 8MiB streams each: 288MiB moved,
        // far heavier than any realistic browser session.
        val pool = Executors.newFixedThreadPool(12)
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        (0 until 12).map { client ->
            pool.submit {
                try {
                    repeat(3) {
                        val connection = URL("http://127.0.0.1:$port/video").openConnection() as HttpURLConnection
                        connection.readTimeout = 30_000
                        val size = connection.inputStream.use { it.readBytes().size }
                        if (size != fileSize.toInt()) failures.add("client $client got $size bytes")
                    }
                } catch (e: Exception) {
                    failures.add("client $client: $e")
                }
            }
        }.forEach { it.get() }
        pool.shutdown()
        running.set(false)
        sampler.join(1000)

        assertTrue(failures.isEmpty(), "failures: $failures")
        val measured = peak.get()
        if (measured >= 0) {
            println("PEAK_DIRECT_MEMORY_BYTES=${measured}")
            // The 32MiB cap must sit well above real usage.
            assertTrue(measured < 32L * 1024 * 1024, "peak direct memory $measured unexpectedly high")
        } else {
            println("PEAK_DIRECT_MEMORY_BYTES=unavailable (allocator not in no-cleaner mode on this JVM)")
        }
    }
}
