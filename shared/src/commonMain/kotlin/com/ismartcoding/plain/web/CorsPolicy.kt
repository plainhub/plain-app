package com.ismartcoding.plain.web

import com.ismartcoding.plain.platform.isDebugBuild

/**
 * Shared CORS policy for the embedded HTTP server.
 *
 * Android applies it through Ktor's `CORS` plugin (see `HttpModule`); iOS
 * applies it manually in `IosRequestProcessor` because its SwiftNIO request
 * pipeline does not run through Ktor. Keeping the allowlist here ensures both
 * platforms enforce the same cross-origin rules — previously iOS silently
 * skipped CORS, so preflight requests were rejected by the browser.
 *
 * Debug builds allow any origin; release builds restrict to the web UI dev
 * servers. Custom headers prefixed with `c-` (e.g. `c-token`) are advertised
 * as allowed in preflight responses.
 */
object CorsPolicy {

    /** Hosts permitted in release builds (debug builds allow any host). */
    val releaseHosts: List<String> = listOf(
        "localhost:3000",
        "127.0.0.1:3000",
        "localhost:4000",
        "127.0.0.1:4000",
    )

    /** Header name prefixes allowed in preflight requests. */
    val allowedHeaderPrefixes: List<String> = listOf("c-")

    /** Headers always allowed without a prefix match. */
    private val defaultAllowedHeaders: Set<String> = setOf(
        "content-type",
        "authorization",
        "accept",
    )

    const val allowedMethods: String = "GET, POST, PUT, DELETE, OPTIONS, HEAD"
    const val maxAgeSeconds: String = "86400"

    /** Whether any origin is accepted (debug builds). */
    val anyHostAllowed: Boolean get() = isDebugBuild()

    /**
     * Whether the given `Origin` header value is permitted. The Origin is
     * parsed as `<scheme>://<host>[:<port>]` and the host portion is matched
     * against [releaseHosts] (case-insensitive). Returns `true` for any
     * origin when [anyHostAllowed].
     */
    fun isOriginAllowed(origin: String): Boolean {
        if (anyHostAllowed) return true
        val afterScheme = origin.substringAfter("://", "")
        if (afterScheme.isEmpty()) return false
        val hostPart = afterScheme.substringBefore('/')
        return releaseHosts.any { it.equals(hostPart, ignoreCase = true) }
    }

    /**
     * Filter the comma-separated `Access-Control-Request-Headers` value to
     * only the names this policy permits (default headers plus any matching
     * [allowedHeaderPrefixes]). Returns null when [requestHeaders] is absent
     * or contains no allowed names, so the caller can omit the response
     * header entirely.
     */
    fun filterAllowedRequestHeaders(requestHeaders: String?): String? {
        if (requestHeaders.isNullOrEmpty()) return null
        val allowed = requestHeaders.split(',')
            .map { it.trim().lowercase() }
            .filter { name ->
                defaultAllowedHeaders.contains(name) ||
                    allowedHeaderPrefixes.any { name.startsWith(it) }
            }
        return if (allowed.isEmpty()) null else allowed.joinToString(", ")
    }
}
