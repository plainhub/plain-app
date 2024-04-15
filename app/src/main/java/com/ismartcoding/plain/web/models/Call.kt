package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.helpers.FileHelper
import kotlinx.datetime.Instant

data class Call(
    var id: ID,
    var number: String,
    var name: String,
    var photoId: String,
    var startedAt: Instant,
    var duration: Int,
    var type: Int,
    val accountId: ID,
    val geo: PhoneGeo?,
)

fun DCall.toModel(): Call {
    return Call(ID(id), number, name, FileHelper.getFileId(photoUri), startedAt, duration, type, ID(accountId), getGeo())
}
