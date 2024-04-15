package com.ismartcoding.plain.data

import android.os.Parcelable
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.data.IMedia
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
@kotlinx.serialization.Serializable
data class DVideo(
    override var id: String,
    val title: String,
    override val path: String,
    override val duration: Long,
    val size: Long,
    val bucketId: String,
) : IData, IMedia, Parcelable, Serializable {
    companion object {
        private const val serialVersionUID = -2797285L
    }
}
