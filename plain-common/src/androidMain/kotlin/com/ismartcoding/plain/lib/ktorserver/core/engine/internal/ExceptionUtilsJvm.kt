/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.engine.internal

import java.util.concurrent.TimeoutException

@Suppress("ACTUAL_WITHOUT_EXPECT")
public typealias ClosedChannelException = java.nio.channels.ClosedChannelException

@Suppress("ACTUAL_WITHOUT_EXPECT")
internal typealias OutOfMemoryError = java.lang.OutOfMemoryError

@Suppress("ACTUAL_WITHOUT_EXPECT")
internal typealias TimeoutException = TimeoutException
