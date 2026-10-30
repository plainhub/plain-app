package com.ismartcoding.plain.rust.host.provider

import kotlinx.serialization.Serializable

@Serializable
internal data class ProviderPageRequest(val plan: ProviderPlan, val limit: Int, val offset: Int)

@Serializable
internal data class FileIndexRowsRequest(val plan: ProviderPlan, val limit: Int, val offset: Int, val sortBy: String)

@Serializable
internal data class MediaPlanRequest(val dataType: String, val plan: ProviderPlan)

@Serializable
internal data class MediaRowsRequest(
    val dataType: String,
    val plan: ProviderPlan,
    val limit: Int,
    val offset: Int,
    val sortBy: String,
    val includeRevision: Boolean? = null,
    val pathsOnly: Boolean = false,
)
