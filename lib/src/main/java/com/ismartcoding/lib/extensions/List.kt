package com.ismartcoding.lib.extensions

import java.util.HashSet

fun <T> MutableList<T>.swap(
    index1: Int,
    index2: Int,
): MutableList<T> {
    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
    return this
}

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
