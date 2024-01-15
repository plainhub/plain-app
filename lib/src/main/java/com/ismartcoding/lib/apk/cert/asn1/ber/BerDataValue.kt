package com.ismartcoding.lib.apk.cert.asn1.ber

import java.nio.ByteBuffer

/**
 * ASN.1 Basic Encoding Rules (BER) data value -- see `X.690`.
 */
class BerDataValue internal constructor(
    private val mEncoded: ByteBuffer,
    private val mEncodedContents: ByteBuffer,
    /**
     * Returns the tag class of this data value. See [BerEncoding] `TAG_CLASS`
     * constants.
     */
    val tagClass: Int,
    /**
     * Returns `true` if the content octets of this data value are the complete BER encoding
     * of one or more data values, `false` if the content octets of this data value directly
     * represent the value.
     */
    val isConstructed: Boolean,
    /**
     * Returns the tag number of this data value. See [BerEncoding] `TAG_NUMBER`
     * constants.
     */
    val tagNumber: Int
) {

    val encoded: ByteBuffer
        /**
         * Returns the encoded form of this data value.
         */
        get() = mEncoded.slice()
    val encodedContents: ByteBuffer
        /**
         * Returns the encoded contents of this data value.
         */
        get() = mEncodedContents.slice()

    /**
     * Returns a new reader of the contents of this data value.
     */
    fun contentsReader(): BerDataValueReader {
        return ByteBufferBerDataValueReader(mEncodedContents)
    }

    /**
     * Returns a new reader which returns just this data value. This may be useful for re-reading
     * this value in different contexts.
     */
    fun dataValueReader(): BerDataValueReader {
        return ParsedValueReader(this)
    }

    private class ParsedValueReader(private val mValue: BerDataValue) : BerDataValueReader {
        private var mValueOutput = false
        @Throws(BerDataValueFormatException::class)
        override fun readDataValue(): BerDataValue? {
            if (mValueOutput) {
                return null
            }
            mValueOutput = true
            return mValue
        }
    }
}