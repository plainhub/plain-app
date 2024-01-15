package com.ismartcoding.lib.apk.utils.xml

import java.io.IOException
import java.io.Writer

/**
 * Executes a sequence of translators one after the other. Execution ends whenever
 * the first translator consumes codepoints from the input.
 *
 */
internal class AggregateTranslator(vararg translators: CharSequenceTranslator?) :
    CharSequenceTranslator() {
    private val translators: Array<CharSequenceTranslator>

    /**
     * Specify the translators to be used at creation time.
     *
     * @param translators CharSequenceTranslator array to aggregate
     */
    init {
        this.translators = translators as Array<CharSequenceTranslator>
    }

    /**
     * The first translator to consume codepoints from the input is the 'winner'.
     * Execution stops with the number of consumed codepoints being returned.
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun translate(input: CharSequence, index: Int, out: Writer): Int {
        for (translator in translators) {
            val consumed = translator.translate(input, index, out)
            if (consumed != 0) {
                return consumed
            }
        }
        return 0
    }
}