package com.ismartcoding.plain.features.share

import kotlinx.serialization.Serializable

@Serializable
data class SharedBatchPlan(
    val targets: List<ShareFileTarget>,
    val totalFiles: Int,
    val totalSize: Long,
)
