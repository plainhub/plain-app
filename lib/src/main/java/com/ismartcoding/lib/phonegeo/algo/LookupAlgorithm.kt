package com.ismartcoding.lib.phonegeo.algo

import com.ismartcoding.lib.phonegeo.PhoneNumberInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class LookupAlgorithm(val data: ByteArray) {
    protected var srcByteBuffer: ByteBuffer = ByteBuffer.wrap(data).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
    protected var indicesStartOffset = 0
    protected var indicesEndOffset = 0

    init {
        @Suppress("UNUSED_VARIABLE")
        val dataVersion = srcByteBuffer.int // dataVersion is not used, but we need ByteBuffer.getInt() to move cursor
        indicesStartOffset = srcByteBuffer.getInt(4)
        indicesEndOffset = srcByteBuffer.capacity()
    }

    abstract fun lookup(phoneNumber: String): PhoneNumberInfo?

    protected fun validPhoneNumber(phoneNumber: String): Boolean {
        if (phoneNumber.length !in 7..11) {
            return false
        }

        if (phoneNumber.substring(0, 7).toIntOrNull() == null) {
            return false
        }

        return true
    }

    enum class IMPL {
        BINARY_SEARCH,
        SEQUENCE,
        BINARY_SEARCH_PROSPECT,
    }
}
