package com.ismartcoding.lib.apk.utils.xml

import java.io.IOException
import java.io.Writer

/**
 * Helper subclass to CharSequenceTranslator to remove unpaired surrogates.
 */
internal class UnicodeUnpairedSurrogateRemover : CodePointTranslator() {
    /**
     * Implementation of translate that throws out unpaired surrogates.
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun translate(codepoint: Int, out: Writer): Boolean {
        return if (codepoint >= Character.MIN_SURROGATE.code && codepoint <= Character.MAX_SURROGATE.code) {
            // It's a surrogate. Write nothing and say we've translated.
            true
        } else {
            // It's not a surrogate. Don't translate it.
            false
        }
    }
}