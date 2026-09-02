/*
 * Copyright 2014-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.http.content

import io.ktor.http.*
import io.ktor.util.*

/**
 * Used for inferring what the server might expect when serializing responses or parsing requests.
 *
 */
public val DefaultContentTypesAttribute: AttributeKey<List<ContentType>> =
    AttributeKey<List<ContentType>>("DefaultContentTypes")
