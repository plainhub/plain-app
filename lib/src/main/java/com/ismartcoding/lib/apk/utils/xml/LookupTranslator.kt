package com.ismartcoding.lib.apk.utils.xml

import java.io.IOException
import java.io.Writer

/**
 * Translates a value using a lookup table.
 */
internal class LookupTranslator(vararg lookup: Array<CharSequence?>) : CharSequenceTranslator() {
    private val lookupMap: HashMap<String, CharSequence?> = HashMap()
    private val shortest: Int
    private val longest: Int

    /**
     * Define the lookup table to be used in translation
     *
     * Note that, as of Lang 3.1, the key to the lookup table is converted to a
     * java.lang.String, while the value remains as a java.lang.CharSequence.
     * This is because we need the key to support hashCode and equals(Object),
     * allowing it to be the key for a HashMap. See LANG-882.
     *
     * @param lookup CharSequence[][] table of size [*][2]
     */
    init {
        var _shortest = Int.MAX_VALUE
        var _longest = 0
        for (seq in lookup) {
            lookupMap[seq[0].toString()] = seq[1]
            val sz = seq[0]!!.length
            if (sz < _shortest) {
                _shortest = sz
            }
            if (sz > _longest) {
                _longest = sz
            }
        }
        shortest = _shortest
        longest = _longest
    }

    /**
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun translate(input: CharSequence, index: Int, out: Writer): Int {
        var max = longest
        if (index + longest > input.length) {
            max = input.length - index
        }
        // descend so as to get a greedy algorithm
        for (i in max downTo shortest) {
            val subSeq = input.subSequence(index, index + i)
            val result = lookupMap[subSeq.toString()]
            if (result != null) {
                out.write(result.toString())
                return i
            }
        }
        return 0
    }
}