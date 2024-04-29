package com.ismartcoding.plain.features.file

import com.ismartcoding.plain.data.IData
import kotlinx.datetime.Instant

data class DFile(
    var name: String,
    var path: String,
    val permission: String,
    val createdAt: Instant?,
    val updatedAt: Instant,
    val size: Long,
    val isDir: Boolean,
    val children: Int,
    val mediaStoreId: String = ""
) : IData {
    override var id: String
        get() = path
        set(value) {
            path = value
        }
}
