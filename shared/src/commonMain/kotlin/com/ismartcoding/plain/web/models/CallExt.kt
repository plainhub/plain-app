package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.data.getGeo
import com.ismartcoding.plain.platform.getFileId

fun DCall.toModel(): Call {
    return Call(ID(id), number, name, getFileId(photoUri), startedAt, duration, type, ID(accountId), getGeo())
}
