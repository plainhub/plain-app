package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.httpserver.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The shared route table is built once per process and dispatched by the HTTP
 * server (Ktor), the future SwiftNIO server and the BLE RPC channel alike.
 * This locks two properties:
 *
 * - Building the registry (routes + all three GraphQL schemas) is
 *   platform-free — it must not grow Android/JVM-framework dependencies, or
 *   the launch warm-up and the BLE path break.
 * - The critical routes stay registered across refactors.
 */
class HttpRouteRegistryTest {
    @Test
    fun sharedRouter_buildsPlatformFree_andKeepsCriticalRoutes() {
        val router = HttpRouteRegistry.router
        val registered = router.entries().map { it.method to it.path }.toSet()

        val critical = listOf(
            HttpMethod.GET to "/health",
            HttpMethod.GET to "/shutdown",
            HttpMethod.POST to "/init",
            HttpMethod.POST to "/graphql",
            HttpMethod.POST to "/peer_graphql",
            HttpMethod.POST to "/guest_graphql",
            HttpMethod.GET to "/fs",
            HttpMethod.POST to "/upload",
        )
        critical.forEach { (method, path) ->
            assertTrue(registered.contains(method to path), "$method $path missing from the shared route table")
        }

        // The warm-up forces these singletons; touching them here verifies the
        // schema build itself is platform-free.
        HttpRouteRegistry.mainGraphQL
        HttpRouteRegistry.peerGraphQL
        HttpRouteRegistry.guestGraphQL
    }
}
