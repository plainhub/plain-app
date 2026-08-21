package com.ismartcoding.plain.data

import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput
import kotlinx.serialization.Serializable

@GraphQLInput
@Serializable
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
        fun create(data: IData): TagRelationStub = when (data) {
            is IItemMetadata -> TagRelationStub(data.id, data.title, data.size)
            else -> TagRelationStub(data.id)
        }
    }
}
