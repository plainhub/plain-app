package com.ismartcoding.lib.apk.utils.xml

import java.io.IOException
import java.io.StringWriter
import java.io.Writer

/**
 * An API for translating text.
 * Its core use is to escape and unescape text. Because escaping and unescaping
 * is completely contextual, the API does not present two separate signatures.
 *
 */
abstract class CharSequenceTranslator {
    /**
     * Translate a set of codepoints, represented by an int index into a CharSequence,
     * into another set of codepoints. The number of codepoints consumed must be returned,
     * and the only IOExceptions thrown must be from interacting with the Writer so that
     * the top level API may reliably ignore StringWriter IOExceptions.
     *
     * @param input CharSequence that is being translated
     * @param index int representing the current point of translation
     * @param out Writer to translate the text to
     * @return int count of codepoints consumed
     * @throws IOException if and only if the Writer produces an IOException
     */
    @Throws(IOException::class)
    abstract fun translate(input: CharSequence, index: Int, out: Writer): Int

    /**
     * Helper for non-Writer usage.
     * @param input CharSequence to be translated
     * @return String output of translation
     */
    fun translate(input: CharSequence?): String? {
        return if (input == null) {
            null
        } else try {
            val writer = StringWriter(input.length * 2)
            translate(input, writer)
            writer.toString()
        } catch (ioe: IOException) {
            // this should never ever happen while writing to a StringWriter
            throw RuntimeException(ioe)
        }
    }

    /**
     * Translate an input onto a Writer. This is intentionally final as its algorithm is
     * tightly coupled with the abstract method of this class.
     *
     * @param input CharSequence that is being translated
     * @param out Writer to translate the text to
     * @throws IOException if and only if the Writer produces an IOException
     */
    @Throws(IOException::class)
    fun translate(input: CharSequence?, out: Writer?) {
        requireNotNull(out) { "The Writer must not be null" }
        if (input == null) {
            return
        }
        var pos = 0
        val len = input.length
        while (pos < len) {
            val consumed = translate(input, pos, out)
            if (consumed == 0) {
                val c = Character.toChars(Character.codePointAt(input, pos))
                out.write(c)
                pos += c.size
                continue
            }
            // contract with translators is that they have to understand codepoints
            // and they just took care of a surrogate pair
            for (pt in 0 until consumed) {
                pos += Character.charCount(Character.codePointAt(input, pos))
            }
        }
    }

    /**
     * Helper method to create a merger of this translator with another set of
     * translators. Useful in customizing the standard functionality.
     *
     * @param translators CharSequenceTranslator array of translators to merge with this one
     * @return CharSequenceTranslator merging this translator with the others
     */
    fun with(vararg translators: CharSequenceTranslator?): CharSequenceTranslator {
        val newArray = arrayOfNulls<CharSequenceTranslator>(translators.size + 1)
        newArray[0] = this
        System.arraycopy(translators, 0, newArray, 1, translators.size)
        return AggregateTranslator(*newArray)
    }

    companion object {
        /**
         *
         * Returns an upper case hexadecimal `String` for the given
         * character.
         *
         * @param codepoint The codepoint to convert.
         * @return An upper case hexadecimal `String`
         */
        fun hex(codepoint: Int): String {
            return Integer.toHexString(codepoint).uppercase()
        }
    }
}