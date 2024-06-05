package com.ismartcoding.lib.extensions

import java.util.HashSet

fun List<String>.getMimeType(): String {
    val mimeGroups = HashSet<String>(size)
    val subtypes = HashSet<String>(size)
    forEach {
        val parts = it.getMimeType().split("/")
        if (parts.size == 2) {
            mimeGroups.add(parts.getOrElse(0) { "" })
            subtypes.add(parts.getOrElse(1) { "" })
        } else {
            return "*/*"
        }
    }

    return when {
        subtypes.size == 1 -> "${mimeGroups.first()}/${subtypes.first()}"
        mimeGroups.size == 1 -> "${mimeGroups.first()}/*"
        else -> "*/*"
    }
}
