package com.ismartcoding.lib.apk.cert.asn1.ber

/**
 * Reader of ASN.1 Basic Encoding Rules (BER) data values.
 *
 *
 * BER data value reader returns data values, one by one, from a source. The interpretation of
 * data values (e.g., how to obtain a numeric value from an INTEGER data value, or how to extract
 * the elements of a SEQUENCE value) is left to clients of the reader.
 */
interface BerDataValueReader {
    /**
     * Returns the next data value or `null` if end of input has been reached.
     *
     * @throws BerDataValueFormatException if the value being read is malformed.
     */
    @Throws(BerDataValueFormatException::class)
    fun readDataValue(): BerDataValue?
}