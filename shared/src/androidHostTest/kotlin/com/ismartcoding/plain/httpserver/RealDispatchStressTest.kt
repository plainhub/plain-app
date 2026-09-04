package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.lib.ktorserver.PlainNettyServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.request.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the REAL [PlainHttpServer.requestHandler] (gate + route table + web
 * assets) under concurrent load. The route table is populated via
 * [HttpRouteRegistry] exactly like production; /fs requests fail auth at the
 * handler level (403/400) but must always produce a response.
 */
class RealDispatchStressTest {
    private var port: Int = 0
    private lateinit var engine: PlainNettyServer

    @BeforeTest
    fun setUp() {
        TempData.desktopAccessEnabled.value = true
        TempData.serviceEnabled.value = true
        engine = PlainNettyServer(
            requestHandler = PlainHttpServer.requestHandler,
        ) {
            runningLimit = 32
            tcpKeepAlive = true
            connector {
                port = 0
                host = "127.0.0.1"
            }
        }
        engine.start(wait = false)
        port = runBlocking { engine.resolvedConnectors().first().port }
    }

    @AfterTest
    fun tearDown() {
        engine.stop(50, 500)
        TempData.desktopAccessEnabled.value = false
        TempData.serviceEnabled.value = false
    }

    @Test
    fun spaAndFsRequests_respondUnderConcurrency() {
        val total = 40
        val completed = AtomicInteger(0)
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        val pool = Executors.newFixedThreadPool(12)
        (0 until total).map { i ->
            pool.submit {
                val path = when (i % 3) {
                    0 -> "/audios"
                    1 -> "/fs?id=xxx&w=96&h=96"
                    else -> "/graphql"
                }
                try {
                    val start = System.currentTimeMillis()
                    val connection = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 10_000
                    if (path == "/graphql") {
                        connection.requestMethod = "POST"
                        connection.doOutput = true
                        connection.setRequestProperty("c-id", "test")
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.outputStream.use { it.write("{}".toByteArray()) }
                    }
                    val code = try {
                        connection.responseCode
                    } finally {
                        try { connection.errorStream?.use { it.readBytes() } } catch (_: Exception) {}
                        try { connection.inputStream?.use { it.readBytes() } } catch (_: Exception) {}
                    }
                    val elapsed = System.currentTimeMillis() - start
                    completed.incrementAndGet()
                    if (elapsed > 5000) failures.add("$path took ${elapsed}ms (code=$code)")
                } catch (e: Exception) {
                    failures.add("req $i ($path) failed: $e")
                }
            }
        }.forEach { it.get() }
        pool.shutdown()
        println("FAILURES: " + failures.take(6).joinToString("\n---\n"))
        assertEquals(total, completed.get(), "not all requests completed")
        assertTrue(failures.isEmpty(), "failures: $failures")
    }
}
