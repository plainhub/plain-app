package com.ismartcoding.lib.apk.utils.xml

/**
 * Class holding various entity data for HTML and XML - generally for use with
 * the LookupTranslator.
 * All arrays are of length [*][2].
 */
object EntityArrays {
    /**
     * Mapping to escape the basic XML and HTML character entities.
     *
     *
     * Namely: `" & < >`
     *
     * @return the mapping table
     */
    fun BASIC_ESCAPE(): Array<String> {
        return BASIC_ESCAPE.clone()
    }

    private val BASIC_ESCAPE = arrayOf("\"", "&quot;", "&", "&amp;", "<", "&lt;", ">", "&gt;")

    /**
     * Mapping to escape the apostrophe character to its XML character entity.
     *
     * @return the mapping table
     */
    fun APOS_ESCAPE(): Array<String> {
        return APOS_ESCAPE.clone()
    }

    private val APOS_ESCAPE = arrayOf("'", "&apos;")
}