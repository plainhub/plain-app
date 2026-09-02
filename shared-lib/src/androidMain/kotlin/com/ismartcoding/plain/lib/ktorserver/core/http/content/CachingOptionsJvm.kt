/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.http.content

import io.ktor.http.*
import io.ktor.http.content.*
import com.ismartcoding.plain.lib.ktorserver.core.util.*
import java.time.*

/**
 * Creates [CachingOptions] instance with [ZonedDateTime] expiration time
 *
 */
public fun CachingOptions(cacheControl: CacheControl? = null, expires: ZonedDateTime): CachingOptions =
    CachingOptions(cacheControl, expires.toGMTDate())
