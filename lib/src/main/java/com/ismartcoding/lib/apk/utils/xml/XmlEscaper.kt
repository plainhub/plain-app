package com.ismartcoding.lib.apk.utils.xml

object XmlEscaper {
    /**
     *
     * Escapes the characters in a `String` using XML entities.
     */
    fun escapeXml10(input: String?): String? {
        return ESCAPE_XML10.translate(input)
    }

    val ESCAPE_XML10: CharSequenceTranslator = AggregateTranslator(
        LookupTranslator(EntityArrays.BASIC_ESCAPE() as Array<CharSequence?>),
        LookupTranslator(EntityArrays.APOS_ESCAPE() as Array<CharSequence?>),
        LookupTranslator(
                arrayOf("\u0000", ""),
                arrayOf("\u0001", ""),
                arrayOf("\u0002", ""),
                arrayOf("\u0003", ""),
                arrayOf("\u0004", ""),
                arrayOf("\u0005", ""),
                arrayOf("\u0006", ""),
                arrayOf("\u0007", ""),
                arrayOf("\u0008", ""),
                arrayOf("\u000b", ""),
                arrayOf("\u000c", ""),
                arrayOf("\u000e", ""),
                arrayOf("\u000f", ""),
                arrayOf("\u0010", ""),
                arrayOf("\u0011", ""),
                arrayOf("\u0012", ""),
                arrayOf("\u0013", ""),
                arrayOf("\u0014", ""),
                arrayOf("\u0015", ""),
                arrayOf("\u0016", ""),
                arrayOf("\u0017", ""),
                arrayOf("\u0018", ""),
                arrayOf("\u0019", ""),
                arrayOf("\u001a", ""),
                arrayOf("\u001b", ""),
                arrayOf("\u001c", ""),
                arrayOf("\u001d", ""),
                arrayOf("\u001e", ""),
                arrayOf("\u001f", ""),
                arrayOf("\ufffe", ""),
                arrayOf("\uffff", "")
        ),
        NumericEntityEscaper.between(0x7f, 0x84),
        NumericEntityEscaper.between(0x86, 0x9f),
        UnicodeUnpairedSurrogateRemover()
    )
}