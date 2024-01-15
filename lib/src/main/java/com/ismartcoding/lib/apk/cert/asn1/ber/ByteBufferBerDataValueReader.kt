package com.ismartcoding.lib.apk.cert.asn1.ber

import java.nio.ByteBuffer

/**
 * [BerDataValueReader] which reads from a [ByteBuffer] containing BER-encoded data
 * values. See `X.690` for the encoding.
 */
class ByteBufferBerDataValueReader(buf: ByteBuffer?) : BerDataValueReader {
    private val mBuf: ByteBuffer

    init {
        if (buf == null) {
            throw NullPointerException("buf == null")
        }
        mBuf = buf
    }

    @Throws(BerDataValueFormatException::class)
    override fun readDataValue(): BerDataValue? {
        val startPosition = mBuf.position()
        if (!mBuf.hasRemaining()) {
            return null
        }
        val firstIdentifierByte = mBuf.get()
        val tagNumber = readTagNumber(firstIdentifierByte)
        val constructed = BerEncoding.isConstructed(firstIdentifierByte)
        if (!mBuf.hasRemaining()) {
            throw BerDataValueFormatException("Missing length")
        }
        val firstLengthByte = mBuf.get().toInt() and 0xff
        val contentsLength: Int
        val contentsOffsetInTag: Int
        if (firstLengthByte and 0x80 == 0) {
            // short form length
            contentsLength = readShortFormLength(firstLengthByte)
            contentsOffsetInTag = mBuf.position() - startPosition
            skipDefiniteLengthContents(contentsLength)
        } else if (firstLengthByte != 0x80) {
            // long form length
            contentsLength = readLongFormLength(firstLengthByte)
            contentsOffsetInTag = mBuf.position() - startPosition
            skipDefiniteLengthContents(contentsLength)
        } else {
            // indefinite length -- value ends with 0x00 0x00
            contentsOffsetInTag = mBuf.position() - startPosition
            contentsLength =
                if (constructed) skipConstructedIndefiniteLengthContents() else skipPrimitiveIndefiniteLengthContents()
        }

        // Create the encoded data value ByteBuffer
        val endPosition = mBuf.position()
        mBuf.position(startPosition)
        val bufOriginalLimit = mBuf.limit()
        mBuf.limit(endPosition)
        val encoded = mBuf.slice()
        mBuf.position(mBuf.limit())
        mBuf.limit(bufOriginalLimit)

        // Create the encoded contents ByteBuffer
        encoded.position(contentsOffsetInTag)
        encoded.limit(contentsOffsetInTag + contentsLength)
        val encodedContents = encoded.slice()
        encoded.clear()
        return BerDataValue(
            encoded,
            encodedContents,
            BerEncoding.getTagClass(firstIdentifierByte),
            constructed,
            tagNumber
        )
    }

    @Throws(BerDataValueFormatException::class)
    private fun readTagNumber(firstIdentifierByte: Byte): Int {
        val tagNumber = BerEncoding.getTagNumber(firstIdentifierByte)
        return if (tagNumber == 0x1f) {
            // high-tag-number form, where the tag number follows this byte in base-128
            // big-endian form, where each byte has the highest bit set, except for the last
            // byte
            readHighTagNumber()
        } else {
            // low-tag-number form
            tagNumber
        }
    }

    @Throws(BerDataValueFormatException::class)
    private fun readHighTagNumber(): Int {
        // Base-128 big-endian form, where each byte has the highest bit set, except for the last
        // byte
        var b: Int
        var result = 0
        do {
            if (!mBuf.hasRemaining()) {
                throw BerDataValueFormatException("Truncated tag number")
            }
            b = mBuf.get().toInt()
            if (result > Int.MAX_VALUE ushr 7) {
                throw BerDataValueFormatException("Tag number too large")
            }
            result = result shl 7
            result = result or (b and 0x7f)
        } while (b and 0x80 != 0)
        return result
    }

    private fun readShortFormLength(firstLengthByte: Int): Int {
        return firstLengthByte and 0x7f
    }

    @Throws(BerDataValueFormatException::class)
    private fun readLongFormLength(firstLengthByte: Int): Int {
        // The low 7 bits of the first byte represent the number of bytes (following the first
        // byte) in which the length is in big-endian base-256 form
        val byteCount = firstLengthByte and 0x7f
        if (byteCount > 4) {
            throw BerDataValueFormatException("Length too large: $byteCount bytes")
        }
        var result = 0
        for (i in 0 until byteCount) {
            if (!mBuf.hasRemaining()) {
                throw BerDataValueFormatException("Truncated length")
            }
            val b = mBuf.get().toInt()
            if (result > Int.MAX_VALUE ushr 8) {
                throw BerDataValueFormatException("Length too large")
            }
            result = result shl 8
            result = result or (b and 0xff)
        }
        return result
    }

    @Throws(BerDataValueFormatException::class)
    private fun skipDefiniteLengthContents(contentsLength: Int) {
        if (mBuf.remaining() < contentsLength) {
            throw BerDataValueFormatException(
                "Truncated contents. Need: " + contentsLength + " bytes, available: "
                        + mBuf.remaining()
            )
        }
        mBuf.position(mBuf.position() + contentsLength)
    }

    @Throws(BerDataValueFormatException::class)
    private fun skipPrimitiveIndefiniteLengthContents(): Int {
        // Contents are terminated by 0x00 0x00
        var prevZeroByte = false
        var bytesRead = 0
        while (true) {
            if (!mBuf.hasRemaining()) {
                throw BerDataValueFormatException(
                    "Truncated indefinite-length contents: $bytesRead bytes read"
                )
            }
            val b = mBuf.get().toInt()
            bytesRead++
            if (bytesRead < 0) {
                throw BerDataValueFormatException("Indefinite-length contents too long")
            }
            prevZeroByte = if (b == 0) {
                if (prevZeroByte) {
                    // End of contents reached -- we've read the value and its terminator 0x00 0x00
                    return bytesRead - 2
                }
                true
            } else {
                false
            }
        }
    }

    @Throws(BerDataValueFormatException::class)
    private fun skipConstructedIndefiniteLengthContents(): Int {
        // Contents are terminated by 0x00 0x00. However, this data value is constructed, meaning it
        // can contain data values which are themselves indefinite length encoded. As a result, we
        // must parse the direct children of this data value to correctly skip over the contents of
        // this data value.
        val startPos = mBuf.position()
        while (mBuf.hasRemaining()) {
            // Check whether the 0x00 0x00 terminator is at current position
            if (mBuf.remaining() > 1 && mBuf.getShort(mBuf.position()).toInt() == 0) {
                val contentsLength = mBuf.position() - startPos
                mBuf.position(mBuf.position() + 2)
                return contentsLength
            }
            // No luck. This must be a BER-encoded data value -- skip over it by parsing it
            readDataValue()
        }
        throw BerDataValueFormatException(
            "Truncated indefinite-length contents: "
                    + (mBuf.position() - startPos) + " bytes read"
        )
    }
}