package com.ismartcoding.plain.data

import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.features.image.DImage

data class TagRelationStub(
    var key: String = "",
    var title: String = "",
    var size: Long = 0,
) {
    fun toTagRelation(
        tagId: String,
        type: DataType,
    ): DTagRelation {
        val stub = this
        return DTagRelation(tagId, stub.key, type.value).apply {
            title = stub.title
            size = stub.size
        }
    }

    companion object {
        fun create(data: IData): TagRelationStub {
            return when (data) {
                is DImage -> {
                    TagRelationStub(data.id, data.title, data.size)
                }
                is DVideo -> {
                    TagRelationStub(data.id, data.title, data.size)
                }
                is DAudio -> {
                    TagRelationStub(data.id, data.title, data.size)
                }
                else -> {
                    TagRelationStub(data.id)
                }
            }
        }
    }
}
