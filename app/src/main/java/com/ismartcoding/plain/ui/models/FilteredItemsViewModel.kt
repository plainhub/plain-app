package com.ismartcoding.plain.ui.models

import androidx.lifecycle.MutableLiveData
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.tag.TagHelper

open class FilteredItemsViewModel : BaseItemsModel() {
    var data: IData? = null
    val trash = MutableLiveData(false)
    var dataType: DataType = DataType.DEFAULT
    var castMode = false

    suspend fun getQuery(): String {
        var query = "$searchQ trash:${trash.value}"
        if (data != null) {
            when (data) {
                is DTag -> {
                    val tagId = (data as DTag).id
                    val ids = withIO { TagHelper.getKeysByTagId(tagId) }
                    query += " ids:${ids.joinToString(",")}"
                }
                is DFeed -> {
                    val feedId = (data as DFeed).id
                    query += " feed_id:$feedId"
                }
                is DType -> {
                    val type = (data as DType).id
                    query += " type:$type"
                }
                is DMediaBucket -> {
                    query += " bucket_id:${(data as DMediaBucket).id}"
                }
            }
        }

        return query
    }
}

data class DType(override var id: String, val titleId: Int, val iconId: Int) : IData

data class DMediaFolders(override var id: String = "") : IData
