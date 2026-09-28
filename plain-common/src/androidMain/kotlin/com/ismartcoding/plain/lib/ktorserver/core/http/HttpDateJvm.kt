/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.http

import java.time.*
import java.time.format.*
import java.time.temporal.*
import java.util.*

/**
 * Format as HTTP date (GMT)
 *
 */
public fun Temporal.toHttpDateString(): String = httpDateFormat.format(this)

private val GreenwichMeanTime: ZoneId = ZoneId.of("GMT")

/**
 * Default HTTP date format
 *
 */
public val httpDateFormat: DateTimeFormatter = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
    .withLocale(Locale.US)
    .withZone(GreenwichMeanTime)!!
