package com.ismartcoding.plain.features.file

import kotlin.time.Instant

data class DFileTask(
    val id: String,
    val type: FileTaskType,
    val status: FileTaskStatus,
    val title: String,
    val error: String,
    val totalBytes: Long,
    val doneBytes: Long,
    val totalItems: Long,
    val doneItems: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedOps: List<FileTaskCompletedOp>,
)
