package com.ismartcoding.lib.apk.cert.asn1.ber

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * [BerDataValueReader] which reads from an [InputStream] returning BER-encoded data
 * values. See `X.690` for the encoding.
 */
class InputStreamBerDataValueReader(`in`: InputStream?) : BerDataValueReader {
    private val mIn: InputStream

    init {
        if (`in` == null) {
            throw NullPointerException("in == null")
        }
        mIn = `in`
    }

    @Throws(BerDataValueFormatException::class)
    override fun readDataValue(): BerDataValue? {
        return readDataValue(mIn)
    }

    private class RecordingInputStream(private val mIn: InputStream) : InputStream() {
        private val mBuf: ByteArrayOutputStream = ByteArrayOutputStream()

        val readBytes: ByteArray
            get() = mBuf.toByteArray()
        val readByteCount: Int
            get() = mBuf.size()

        @Throws(IOException::class)
        override fun read(): Int {
            val b = mIn.read()
            if (b != -1) {
                mBuf.write(b)
            }
            return b
        }

        @Throws(IOException::class)
        override fun read(b: ByteArray): Int {
            val len = mIn.read(b)
            if (len > 0) {
                mBuf.write(b, 0, len)
            }
            return len
        }

        @Throws(IOException::class)
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            var len = len
            len = mIn.read(b, off, len)
            if (len > 0) {
                mBuf.write(b, off, len)
            }
            return len
        }

        @Throws(IOException::class)
        override fun skip(n: Long): Long {
            if (n <= 0) {
                return 0
            }
            val buf = ByteArray(4096)
            val len = mIn.read(buf, 0, Math.min(buf.size.toLong(), n).toInt())
            if (len > 0) {
                mBuf.write(buf, 0, len)
            }
            return Math.max(len, 0).toLong()
        }

        @Throws(IOException::class)
        override fun available(): Int {
            return super.available()
        }

        @Throws(IOException::class)
        override fun close() {
            super.close()
        }

        @Synchronized
        override fun mark(readlimit: Int) {
        }

        @Synchronized
        @Throws(IOException::class)
        override fun reset() {
            throw IOException("mark/reset not supported")
        }

        override fun markSupported(): Boolean {
            return false
        }
    }

    companion object {
        /**
         * Returns the next data value or `null` if end of input has been reached.
         *
         * @throws BerDataValueFormatException if the value being read is malformed.
         */
        @Throws(BerDataValueFormatException::class)
        private fun readDataValue(input: InputStream): BerDataValue? {
            val `in` = RecordingInputStream(input)
            return try {
                val firstIdentifierByte = `in`.read()
                if (firstIdentifierByte == -1) {
                    // End of input
                    return null
                }
                val tagNumber = readTagNumber(`in`, firstIdentifierByte)
                val firstLengthByte = `in`.read()
                if (firstLengthByte == -1) {
                    throw BerDataValueFormatException("Missing length")
                }
                val constructed = BerEncoding.isConstructed(firstIdentifierByte.toByte())
                val contentsLength: Int
                val contentsOffsetInDataValue: Int
                if (firstLengthByte and 0x80 == 0) {
                    // short form length
                    contentsLength = readShortFormLength(firstLengthByte)
                    contentsOffsetInDataValue = `in`.readByteCount
                    skipDefiniteLengthContents(`in`, contentsLength)
                } else if (firstLengthByte and 0xff != 0x80) {
                    // long form length
                    contentsLength = readLongFormLength(`in`, firstLengthByte)
                    contentsOffsetInDataValue = `in`.readByteCount
                    skipDefiniteLengthContents(`in`, contentsLength)
                } else {
                    // indefinite length
                    contentsOffsetInDataValue = `in`.readByteCount
                    contentsLength =
                        if (constructed) skipConstructedIndefiniteLengthContents(`in`) else skipPrimitiveIndefiniteLengthContents(
                            `in`
                        )
                }
                val encoded: ByteArray = `in`.readBytes
                val encodedContents =
                    ByteBuffer.wrap(encoded, contentsOffsetInDataValue, contentsLength)
                BerDataValue(
                    ByteBuffer.wrap(encoded),
                    encodedContents,
                    BerEncoding.getTagClass(firstIdentifierByte.toByte()),
                    constructed,
                    tagNumber
                )
            } catch (e: IOException) {
                throw BerDataValueFormatException("Failed to read data value", e)
            }
        }

        @Throws(IOException::class, BerDataValueFormatException::class)
        private fun readTagNumber(`in`: InputStream, firstIdentifierByte: Int): Int {
            val tagNumber = BerEncoding.getTagNumber(firstIdentifierByte.toByte())
            return if (tagNumber == 0x1f) {
                // high-tag-number form
                readHighTagNumber(`in`)
            } else {
                // low-tag-number form
                tagNumber
            }
        }

        @Throws(IOException::class, BerDataValueFormatException::class)
        private fun readHighTagNumber(`in`: InputStream): Int {
            // Base-128 big-endian form, where each byte has the highest bit set, except for the last
            // byte where the highest bit is not set
            var b: Int
            var result = 0
            do {
                b = `in`.read()
                if (b == -1) {
                    throw BerDataValueFormatException("Truncated tag number")
                }
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

        @Throws(IOException::class, BerDataValueFormatException::class)
        private fun readLongFormLength(`in`: InputStream, firstLengthByte: Int): Int {
            // The low 7 bits of the first byte represent the number of bytes (following the first
            // byte) in which the length is in big-endian base-256 form
            val byteCount = firstLengthByte and 0x7f
            if (byteCount > 4) {
                throw BerDataValueFormatException("Length too large: $byteCount bytes")
            }
            var result = 0
            for (i in 0 until byteCount) {
                val b = `in`.read()
                if (b == -1) {
                    throw BerDataValueFormatException("Truncated length")
                }
                if (result > Int.MAX_VALUE ushr 8) {
                    throw BerDataValueFormatException("Length too large")
                }
                result = result shl 8
                result = result or (b and 0xff)
            }
            return result
        }

        @Throws(IOException::class, BerDataValueFormatException::class)
        private fun skipDefiniteLengthContents(`in`: InputStream, len: Int) {
            var len = len
            var bytesRead: Long = 0
            while (len > 0) {
                val skipped = `in`.skip(len.toLong()).toInt()
                if (skipped <= 0) {
                    throw BerDataValueFormatException(
                        "Truncated definite-length contents: " + bytesRead + " bytes read"
                                + ", " + len + " missing"
                    )
                }
                len -= skipped
                bytesRead += skipped.toLong()
            }
        }

        @Throws(IOException::class, BerDataValueFormatException::class)
        private fun skipPrimitiveIndefiniteLengthContents(`in`: InputStream): Int {
            // Contents are terminated by 0x00 0x00
            var prevZeroByte = false
            var bytesRead = 0
            while (true) {
                val b = `in`.read()
                if (b == -1) {
                    throw BerDataValueFormatException(
                        "Truncated indefinite-length contents: $bytesRead bytes read"
                    )
                }
                bytesRead++
                if (bytesRead < 0) {
                    throw BerDataValueFormatException("Indefinite-length contents too long")
                }
                if (b == 0) {
                    if (prevZeroByte) {
                        // End of contents reached -- we've read the value and its terminator 0x00 0x00
                        return bytesRead - 2
                    }
                    prevZeroByte = true
                    continue
                } else {
                    prevZeroByte = false
                }
            }
        }

        @Throws(BerDataValueFormatException::class)
        private fun skipConstructedIndefiniteLengthContents(`in`: RecordingInputStream): Int {
            // Contents are terminated by 0x00 0x00. However, this data value is constructed, meaning it
            // can contain data values which are indefinite length encoded as well. As a result, we
            // must parse the direct children of this data value to correctly skip over the contents of
            // this data value.
            val readByteCountBefore: Int = `in`.readByteCount
            while (true) {
                // We can't easily peek for the 0x00 0x00 terminator using the provided InputStream.
                // Thus, we use the fact that 0x00 0x00 parses as a data value whose encoded form we
                // then check below to see whether it's 0x00 0x00.
                val dataValue = readDataValue(`in`)
                    ?: throw BerDataValueFormatException(
                        "Truncated indefinite-length contents: "
                                + (`in`.readByteCount - readByteCountBefore) + " bytes read"
                    )
                if (`in`.readByteCount <= 0) {
                    throw BerDataValueFormatException("Indefinite-length contents too long")
                }
                val encoded = dataValue.encoded
                if (encoded.remaining() == 2 && encoded[0].toInt() == 0 && encoded[1].toInt() == 0) {
                    // 0x00 0x00 encountered
                    return `in`.readByteCount - readByteCountBefore - 2
                }
            }
        }
    }
}