package com.ismartcoding.lib.apk.utils.xml

import java.io.IOException
import java.io.Writer

/**
 * Helper subclass to CharSequenceTranslator to allow for translations that
 * will replace up to one character at a time.
 */
internal abstract class CodePointTranslator : CharSequenceTranslator() {
    /**
     * Implementation of translate that maps onto the abstract translate(int, Writer) method.
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun translate(input: CharSequence, index: Int, out: Writer): Int {
        val codepoint = Character.codePointAt(input, index)
        val consumed = translate(codepoint, out)
        return if (consumed) 1 else 0
    }

    /**
     * Translate the specified codepoint into another.
     *
     * @param codepoint int character input to translate
     * @param out Writer to optionally push the translated output to
     * @return boolean as to whether translation occurred or not
     * @throws IOException if and only if the Writer produces an IOException
     */
    @Throws(IOException::class)
    abstract fun translate(codepoint: Int, out: Writer): Boolean
}