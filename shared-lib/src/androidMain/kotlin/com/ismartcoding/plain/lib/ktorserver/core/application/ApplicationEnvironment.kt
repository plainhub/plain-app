/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.application

import io.ktor.events.*
import com.ismartcoding.plain.lib.ktorserver.core.config.*
import io.ktor.util.logging.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * Represents an environment in which [Application] runs
 *
 */
public interface ApplicationEnvironment {

    /**
     * [ClassLoader] used to load application.
     *
     * Useful for various reflection-based services, like dependency injection.
     *
     */
    public val classLoader: ClassLoader

    /**
     * Instance of [Logger] to be used for logging.
     *
     */
    public val log: Logger

    /**
     * Configuration for the [Application]
     *
     */
    public val config: ApplicationConfig

    /**
     * Provides events on Application lifecycle
     *
     */
    @Deprecated(
        message = "Moved to Application",
        replaceWith = ReplaceWith("EmbeddedServer.monitor", "io.ktor.server.engine.EmbeddedServer"),
        level = DeprecationLevel.WARNING,
    )
    public val monitor: Events
}

internal class ApplicationRootConfigBridge constructor(
    rootConfig: ServerConfig,
    parentCoroutineContext: CoroutineContext,
) {
    val parentCoroutineContext: CoroutineContext = when {
        rootConfig.developmentMode && rootConfig.watchPaths.isNotEmpty() ->
            parentCoroutineContext + ClassLoaderAwareContinuationInterceptor

        else -> parentCoroutineContext
    }
}

private object ClassLoaderAwareContinuationInterceptor : ContinuationInterceptor {
    override val key: CoroutineContext.Key<*> = ContinuationInterceptor.Key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        val classLoader = Thread.currentThread().contextClassLoader
        return object : Continuation<T> {
            override val context: CoroutineContext = continuation.context

            override fun resumeWith(result: Result<T>) {
                Thread.currentThread().contextClassLoader = classLoader
                continuation.resumeWith(result)
            }
        }
    }
}
