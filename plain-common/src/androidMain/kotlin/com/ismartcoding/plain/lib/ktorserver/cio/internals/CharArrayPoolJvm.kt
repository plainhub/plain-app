/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
/**
 * Vendored from io.ktor:ktor-http-cio:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.cio.internals

internal fun isPoolingDisabled(): Boolean =
    System.getProperty("ktor.internal.cio.disable.chararray.pooling")?.toBoolean() ?: false
