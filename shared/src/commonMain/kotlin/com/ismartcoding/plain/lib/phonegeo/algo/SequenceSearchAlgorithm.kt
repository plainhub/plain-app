package com.ismartcoding.plain.lib.phonegeo.algo

import com.ismartcoding.plain.lib.phonegeo.ISP
import com.ismartcoding.plain.lib.phonegeo.LittleEndianReader
import com.ismartcoding.plain.lib.phonegeo.PhoneGeoInfo
import com.ismartcoding.plain.lib.phonegeo.PhoneNumberInfo

class SequenceSearchAlgorithm(data: ByteArray) : LookupAlgorithm(data) {
    override fun lookup(phoneNumber: String): PhoneNumberInfo? {
        if (!validPhoneNumber(phoneNumber)) {
            return null
        }
        val byteBuffer = srcByteBuffer.copy()
        val geoId =
            try {
                phoneNumber.substring(0, 7).toInt()
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("phone number $phoneNumber is invalid, is it numeric")
            }
        for (i in indicesStartOffset until byteBuffer.limit step 8 + 1) {
            byteBuffer.position = i
            val phonePrefix = byteBuffer.getInt()
            val indicesPhoneStart = byteBuffer.getInt()
            val ispCode = byteBuffer.get()
            if (phonePrefix == geoId) {
                val isp = ISP.Companion.of(ispCode.toInt())
                byteBuffer.position = indicesPhoneStart
                while (byteBuffer.get() != 0.toByte()) {
                }
                val indicesPhoneEnd = byteBuffer.position - 1
                byteBuffer.position = indicesPhoneStart
                val length = indicesPhoneEnd - indicesPhoneStart
                val bytes = ByteArray(length)
                byteBuffer.get(bytes, 0, length)
                val oriString = bytes.decodeToString()
                val parts = oriString.split("|")
                val phoneGeoInfo = PhoneGeoInfo(parts[0], parts[1], parts[2], parts[3])
                return PhoneNumberInfo(phoneNumber, phoneGeoInfo, isp)
            }
        }
        return null
    }
}
