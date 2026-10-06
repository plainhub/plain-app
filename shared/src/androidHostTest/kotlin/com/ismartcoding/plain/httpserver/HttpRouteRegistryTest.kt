package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.httpserver.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Rust public listener owns the bulk of the HTTP surface and answers those
 * requests itself, so [HttpRouteRegistry] is only the fallback table behind
 * the bridge. This locks the split in both directions:
 *
 * - Building the registry stays platform-free — it must not grow
 *   Android/JVM-framework dependencies, or the launch warm-up breaks.
 * - The exact set of Kotlin-handled routes. Routes missing here are ones
 *   Rust must serve without forwarding; a route listed here is one the bridge
 *   has to keep alive, and losing it silently breaks that protocol.
 * - Routes Rust owns must not reappear here: a second implementation behind
 *   the bridge is unreachable at best and a divergent authority at worst.
 */
class HttpRouteRegistryTest {
    private val servedByRust = listOf(
        HttpMethod.GET to "/health",
        HttpMethod.GET to "/shutdown",
        HttpMethod.POST to "/init",
        HttpMethod.POST to "/graphql",
        HttpMethod.POST to "/peer_graphql",
        HttpMethod.POST to "/guest_graphql",
        HttpMethod.POST to "/nearby",
        HttpMethod.GET to "/proxyfs",
        HttpMethod.POST to "/upload",
        HttpMethod.POST to "/upload_chunk",
        HttpMethod.GET to "/zip/dir",
        HttpMethod.GET to "/zip/files",
        HttpMethod.GET to "/description.xml",
        HttpMethod.POST to "/AVTransport/control",
        HttpMethod.POST to "/RenderingControl/control",
    )

    @Test
    fun sharedRouter_holdsExactlyTheRoutesRustHasNotTakenOver() {
        val registered = HttpRouteRegistry.router.entries().map { it.method to it.path }.toSet()

        assertEquals(
            setOf(
                HttpMethod.GET to "/fs",
                HttpMethod.GET to "/media/{id}",
                HttpMethod("NOTIFY") to "/callback/cast",
            ),
            registered,
            "the bridge fallback table changed — /fs (mobile id + sid) and the DLNA sender routes are " +
                "still Kotlin-only, so dropping one breaks that protocol",
        )
    }

    @Test
    fun sharedRouter_doesNotReAddRoutesRustServesDirectly() {
        val registered = HttpRouteRegistry.router.entries().map { it.method to it.path }.toSet()
        servedByRust.forEach { (method, path) ->
            assertTrue(
                (method to path) !in registered,
                "$method $path is served by the Rust public listener without forwarding — " +
                    "re-registering it here can only create a second, unreachable implementation",
            )
        }
    }

    @Test
    fun graphQlSchemas_stayPlatformFree() {
        HttpRouteRegistry.peerGraphQL
        HttpRouteRegistry.guestGraphQL
    }
}