package com.ismartcoding.plain.api

import kotlinx.serialization.Serializable

@Serializable
internal data class ContentQueryRequest(val query: String, val variables: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap())
