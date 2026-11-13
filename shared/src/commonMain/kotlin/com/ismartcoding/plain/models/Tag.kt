package com.ismartcoding.plain.models

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.platform.system.GraphqlDataTypeSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    override var id: String,
    var name: String,
    var count: Int,
    @Serializable(with = GraphqlDataTypeSerializer::class)
    var type: DataType = DataType.DEFAULT,
) : IData
