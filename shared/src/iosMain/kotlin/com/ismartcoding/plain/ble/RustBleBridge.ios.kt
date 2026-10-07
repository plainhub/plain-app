package com.ismartcoding.plain.ble

import com.ismartcoding.plain.rustprefs.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
internal actual object RustBleBridge {
    actual fun encoder(data: ByteArray, requestId: Int, response: Boolean, limit: Int): Long = data.usePinned { pinned ->
        val handle = plain_ble_encoder(requestId.toUInt(), response, limit.toUInt(), if (data.isEmpty()) null else pinned.addressOf(0).reinterpret(), data.size.toULong()).toLong()
        check(handle != 0L) { "Invalid BLE encoder parameters" }
        handle
    }
    actual fun create(): Long = plain_ble_new().toLong()
    actual fun free(handle: Long) = plain_ble_free(handle.toULong())
    actual fun info(handle: Long): Long = plain_ble_info(handle.toULong()).toLong()
    actual fun call(action: Int, handle: Long, requestId: Int, sequence: Int, limit: Int, response: Boolean, data: ByteArray): ByteArray? = data.usePinned { pinned ->
        val buffer = plain_ble_call(action, handle.toULong(), requestId.toUInt(), sequence.toUInt(), limit.toUInt(), response,
            if (data.isEmpty()) null else pinned.addressOf(0).reinterpret(), data.size.toULong())
        try {
            buffer.useContents {
                if (status == 0) return@useContents null
                val value = this.data?.reinterpret<ByteVar>()?.readBytes(len.toInt()) ?: ByteArray(0)
                check(status > 0) { value.decodeToString() }
                value
            }
        } finally { plain_ble_buffer_free(buffer) }
    }
}
