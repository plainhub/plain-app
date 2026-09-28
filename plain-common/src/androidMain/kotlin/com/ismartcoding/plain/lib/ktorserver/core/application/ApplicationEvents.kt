/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.application

/**
 * Provides events for [Application] lifecycle.
 *
 */
@Deprecated(
    "ApplicationEvents has been renamed to Events.",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Events", "com.ismartcoding.plain.lib.ktorserver.events.Events")
)
public typealias ApplicationEvents = com.ismartcoding.plain.lib.ktorserver.events.Events

/**
 * Specifies signature for the event handler.
 *
 */
@Deprecated(
    "EventHandler has been moved to package com.ismartcoding.plain.lib.ktorserver.events",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("EventHandler<T>", "com.ismartcoding.plain.lib.ktorserver.events.EventHandler")
)
public typealias EventHandler<T> = com.ismartcoding.plain.lib.ktorserver.events.EventHandler<T>

/**
 * Definition of an event.
 * Event is used as a key so both [hashCode] and [equals] need to be implemented properly.
 * Inheriting of this class is an experimental plugin.
 * Instantiate directly if inheritance not necessary.
 *
 *
 *
 * @param T specifies what is a type of a value passed to the event
 */
@Deprecated(
    "EventDefinition<T> has been moved to com.ismartcoding.plain.lib.ktorserver.events",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("EventDefinition<T>", "com.ismartcoding.plain.lib.ktorserver.events.EventDefinition")
)
public typealias EventDefinition<T> = com.ismartcoding.plain.lib.ktorserver.events.EventDefinition<T>
