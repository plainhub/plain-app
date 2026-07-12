@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.lib

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.posix.memcpy

fun ByteArray.toNSData(): NSData {
    return if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
        }
    }
}

fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
}
