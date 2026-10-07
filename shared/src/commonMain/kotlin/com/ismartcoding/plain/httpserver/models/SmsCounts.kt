package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.platform.DSmsCounts

data class SmsCounts(
    val total: Int,
    val inbox: Int,
    val sent: Int,
    val drafts: Int,
)

fun DSmsCounts.toModel(): SmsCounts {
    return SmsCounts(total, inbox, sent, drafts)
}