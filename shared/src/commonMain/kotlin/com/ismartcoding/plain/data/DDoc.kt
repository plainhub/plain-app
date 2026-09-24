package com.ismartcoding.plain.data

import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.db.IMedia
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import kotlin.time.Instant

data class DDoc(
    override var id: String,
    override var title: String,
    override var path: String,
    override val durationMs: Long,
    val size: Long,
    val bucketId: String = "",
    val createdAt: Instant,
    val updatedAt: Instant,
) : IMedia, IData {
    val extension: String by lazy { path.getFilenameExtension() }
}