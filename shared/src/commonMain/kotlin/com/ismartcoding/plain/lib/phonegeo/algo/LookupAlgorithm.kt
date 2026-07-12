package com.ismartcoding.plain.lib.phonegeo.algo

import com.ismartcoding.plain.lib.phonegeo.LittleEndianReader
import com.ismartcoding.plain.lib.phonegeo.PhoneNumberInfo

abstract class LookupAlgorithm(val data: ByteArray) {
    protected var srcByteBuffer: LittleEndianReader = LittleEndianReader(data)
    protected var indicesStartOffset = 0
    protected var indicesEndOffset = 0

    init {
        @Suppress("UNUSED_VARIABLE")
        val dataVersion = srcByteBuffer.getInt() // dataVersion is not used, but we need to move cursor
        indicesStartOffset = srcByteBuffer.getInt(4)
        indicesEndOffset = srcByteBuffer.capacity
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
