package com.ismartcoding.plain.api

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
internal data class RustHostReply(
    val id: Long,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val result: JsonElement? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val error: String? = null,
)
