package com.ismartcoding.plain.features.image

import android.os.Parcelable
import com.ismartcoding.plain.data.IData
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class DImage(
    override var id: String,
    val title: String,
    val path: String,
    val size: Long,
    val bucketId: String,
) : IData, Parcelable, Serializable {
    companion object {
        private const  val serialVersionUID = -76L
    }
}
