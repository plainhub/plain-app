/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ismartcoding.plain.lib.ktorserver

import io.ktor.http.*

/**
 * Merge a list of cache control directives.
 *
 * Vendored from io.ktor:ktor-server-caching-headers:3.5.2.
 */
internal fun List<CacheControl>.mergeCacheControlDirectives(): List<CacheControl> {
    if (size < 2) return this

    val visibility = when {
        any { it.visibility == CacheControl.Visibility.Private } -> CacheControl.Visibility.Private
        any { it.visibility == CacheControl.Visibility.Public } -> CacheControl.Visibility.Public
        else -> null
    }

    val noCacheDirective = firstOrNull { it is CacheControl.NoCache } as CacheControl.NoCache?
    val noStoreDirective = firstOrNull { it is CacheControl.NoStore } as CacheControl.NoStore?

    val maxAgeDirectives = filterIsInstance<CacheControl.MaxAge>()
    val minMaxAge = maxAgeDirectives.minByOrNull { it.maxAgeSeconds }?.maxAgeSeconds
    val minProxyMaxAge = maxAgeDirectives.minByOrNull { it.proxyMaxAgeSeconds ?: Int.MAX_VALUE }?.proxyMaxAgeSeconds
    val mustRevalidate = maxAgeDirectives.any { it.mustRevalidate }
    val proxyRevalidate = maxAgeDirectives.any { it.proxyRevalidate }

    return mutableListOf<CacheControl>().apply {
        noCacheDirective?.let { add(CacheControl.NoCache(null)) }
        noStoreDirective?.let { add(CacheControl.NoStore(null)) }

        this@mergeCacheControlDirectives
            .filter {
                it !is CacheControl.NoCache && it !is CacheControl.NoStore && it !is CacheControl.MaxAge
            }
            .let { addAll(it) }

        if (maxAgeDirectives.isNotEmpty()) {
            add(
                CacheControl.MaxAge(
                    minMaxAge!!,
                    minProxyMaxAge,
                    mustRevalidate,
                    proxyRevalidate,
                    visibility
                )
            )
        } else {
            when {
                noCacheDirective != null -> set(0, CacheControl.NoCache(visibility))
                noStoreDirective != null -> set(0, CacheControl.NoStore(visibility))
            }
        }
    }
}
