package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.enums.CallType
import com.ismartcoding.plain.data.getGeo
import com.ismartcoding.plain.helpers.getFileId

fun DCall.toModel(): Call {
    return Call(ID(id), number, name, getFileId(photoUri), startedAt, durationSec = durationSec, type = CallType.fromInt(type), accountId = ID(accountId), geo = getGeo())
}
