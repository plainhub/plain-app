package com.ismartcoding.plain.docs

import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.data.IMedia
import kotlin.time.Instant

data class DDoc(
    override var id: String,
    override var title: String,
    override var path: String,
    override val duration: Long,
    val size: Long,
    val bucketId: String = "",
    val createdAt: Instant,
    val updatedAt: Instant,
) : IMedia, IData {
    val extension: String by lazy { path.getFilenameExtension() }
}
