/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.util

import io.ktor.utils.io.*

@OptIn(InternalAPI::class)
public typealias CopyOnWriteHashMap<K, V> = io.ktor.util.collections.CopyOnWriteHashMap<K, V>
