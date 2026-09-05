package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.isDebugBuild

/**
 * Shared CORS policy for the embedded HTTP server.
 *
 * Android applies it through Ktor's `CORS` plugin (see `HttpModule`); iOS
 * applies it manually in `IosRequestProcessor` because its SwiftNIO request
 * pipeline does not run through Ktor. Keeping the policy here ensures both
 * platforms enforce the same cross-origin rules — previously iOS silently
 * skipped CORS, so preflight requests were rejected by the browser.
 *
 * There is no hardcoded origin allowlist: debug builds always accept any
 * origin, and release builds only accept any origin when the user enables
 * "Allow any host" in Developer settings ([TempData.allowAnyHost]).
 * Same-origin requests skip CORS processing (Ktor's
 * `allowSameOrigin` default of `true`). Custom headers prefixed with `c-`
 * (e.g. `c-token`) are advertised as allowed in preflight responses.
 */
object CorsPolicy {
    val allowedHeaderPrefixes: List<String> = listOf("c-")

    /** Headers always allowed without a prefix match. */
    private val defaultAllowedHeaders: Set<String> = setOf(
        "content-type",
        "authorization",
        "accept",
    )

    const val allowedMethods: String = "GET, POST, PUT, DELETE, OPTIONS, HEAD"
    const val maxAgeSeconds: String = "86400"

    /**
     * Cross-origin isolation headers appended to every HTTP response. Chromium
     * needs a cross-origin isolated document (`crossOriginIsolated`) for the
     * hardware-accelerated WebCodecs paths used by screen mirror; Safari does
     * not support `credentialless` COEP and simply stays non-isolated.
     * `credentialless` is preferred over `require-corp` because it does not
     * block cross-origin subresources (feed images, avatars). Only top-level
     * document responses consume these headers — appending them to API and
     * asset responses is harmless and mirrors the desktop dev server.
     */
    const val crossOriginOpenerPolicy: String = "same-origin"
    const val crossOriginEmbedderPolicy: String = "credentialless"

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
