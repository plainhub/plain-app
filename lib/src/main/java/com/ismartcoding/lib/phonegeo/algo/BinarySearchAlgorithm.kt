package com.ismartcoding.lib.phonegeo.algo

import com.ismartcoding.lib.phonegeo.ISP
import com.ismartcoding.lib.phonegeo.PhoneGeoInfo
import com.ismartcoding.lib.phonegeo.PhoneNumberInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class BinarySearchAlgorithm(data: ByteArray) : LookupAlgorithm(data) {
    override fun lookup(phoneNumber: String): PhoneNumberInfo? {
        if (!validPhoneNumber(phoneNumber)) {
            return null
        }
        val byteBuffer = srcByteBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val geoId =
            try {
                phoneNumber.substring(0, 7).toInt()
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("phone number %s is invalid, is it numeric".format(phoneNumber))
            }
        var left = indicesStartOffset
        var right = indicesEndOffset
        var mid = indicesStartOffset + (indicesEndOffset - indicesStartOffset) / 2
        mid = alignPosition(mid)
        while (mid in left..right) {
            if (mid == right) return null
            val compare = compare(mid, geoId, byteBuffer)
            when {
                compare == 0 -> return extract(phoneNumber, mid, byteBuffer)
                mid == left -> return null
                compare > 0 -> {
                    val tmp = (left + mid) / 2
                    right = mid
                    mid = alignPosition(tmp)
                }
                else -> {
                    val tmp = (mid + right) / 2
                    left = mid
                    mid = alignPosition(tmp)
                }
            }
        }
        return null
    }

    protected fun alignPosition(position: Int): Int {
        val remain = (position - indicesStartOffset) % 9
        return if ((position - indicesStartOffset) < 9) {
            position - indicesStartOffset
        } else if (remain != 0) {
            position + 9 - remain
        } else {
            position
        }
    }

    protected fun compare(
        position: Int,
        key: Int,
        byteBuffer: ByteBuffer,
    ): Int {
        byteBuffer.position(position)
        val phoneNumberPrefix = byteBuffer.int
        return phoneNumberPrefix.compareTo(key)
    }

    protected fun parseGeo(src: String): PhoneGeoInfo {
        val geos = src.split("|")
        if (geos.size < 4) {
            throw IllegalStateException("Content format error")
        }
        return PhoneGeoInfo(geos[0], geos[1], geos[2], geos[3])
    }

    protected fun detectInfoLength(
        infoStartIndex: Int,
        byteBuffer: ByteBuffer,
    ): Int {
        byteBuffer.position(infoStartIndex)
        while (byteBuffer.get() != 0.toByte()) {
        }
        val infoEndIndex = byteBuffer.position() - 1
        byteBuffer.position(infoStartIndex)
        return infoEndIndex - infoStartIndex
    }

    protected fun extract(
        phoneNumber: String,
        start: Int,
        byteBuffer: ByteBuffer,
    ): PhoneNumberInfo {
        byteBuffer.position(start)
        @Suppress("UNUSED_VARIABLE")
        val prefix = byteBuffer.int // prefix is not used, but we need ByteBuffer.getInt() to move cursor
        val infoStartIndex = byteBuffer.int
        val ispCode = byteBuffer.get()

        val isp = ISP.of(ispCode.toInt())
        val bytes = ByteArray(detectInfoLength(infoStartIndex, byteBuffer))
        byteBuffer.get(bytes)
        val geoString = String(bytes)
        val geoInfo = parseGeo(geoString)
        return PhoneNumberInfo(phoneNumber, geoInfo, isp)
    }
}
