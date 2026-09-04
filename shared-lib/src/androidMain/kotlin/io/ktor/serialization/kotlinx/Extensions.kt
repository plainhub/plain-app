/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.serialization.kotlinx

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import kotlinx.serialization.*

/**
 * Vendored from io.ktor:ktor-serialization-kotlinx:3.5.2 (JVM actual merged; ServiceLoader lookup
 * replaced with a fixed provider list).
 */
internal val providers: List<KotlinxSerializationExtensionProvider> =
    listOf(KotlinxSerializationJsonExtensionProvider())

internal fun extensions(format: SerialFormat): List<KotlinxSerializationExtension> =
    providers.mapNotNull { it.extension(format) }

/**
 * A factory for [KotlinxSerializationExtension]
 */
public interface KotlinxSerializationExtensionProvider {
    public fun extension(format: SerialFormat): KotlinxSerializationExtension?
}

/**
 * An extension for [KotlinxSerializationConverter] that can add format-specific logic
 */
public interface KotlinxSerializationExtension {

    public suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): OutgoingContent?

    public suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any?
}
