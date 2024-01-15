package com.ismartcoding.lib.apk.utils

object Strings {
    /**
     * Copied fom commons StringUtils
     *
     * Joins the elements of the provided `Iterable` into
     * a single String containing the provided elements.
     */
    @JvmStatic
    fun join(iterable: Iterable<*>, separator: String?): String? {
        return join(iterable.iterator(), separator)
    }

    /**
     * Copied fom commons StringUtils
     */
    fun join(iterator: Iterator<*>, separator: String?): String? {

        if (!iterator.hasNext()) {
            return ""
        }
        val first = iterator.next()
        if (!iterator.hasNext()) {
            return first?.toString()
        }
        // two or more elements
        val buf = StringBuilder(256)
        // Java default is 16, probably too small
        if (first != null) {
            buf.append(first)
        }
        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator)
            }
            val obj = iterator.next()
            if (obj != null) {
                buf.append(obj)
            }
        }
        return buf.toString()
    }

    @JvmStatic
    fun isNumeric(cs: CharSequence): Boolean {
        if (cs.isEmpty()) {
            return false
        }
        return cs.find { !it.isDigit() } == null
    }

    @JvmStatic
    fun substringBefore(str: String, separator: String): String {
        if (str.isEmpty()) {
            return str
        }
        if (separator.isEmpty()) {
            return ""
        }
        val pos = str.indexOf(separator)
        return if (pos == -1) {
            str
        } else str.substring(0, pos)
    }
}