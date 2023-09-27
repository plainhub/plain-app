package com.ismartcoding.plain.features.wireguard.config

import java.util.regex.Pattern

class Attribute(val key: String, val value: String) {
    companion object {
        private val LINE_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*([^\\s#][^#]*)")
        private val LIST_SEPARATOR = Pattern.compile("\\s*,\\s*")

        fun parse(line: CharSequence): Attribute? {
            val matcher = LINE_PATTERN.matcher(line)
            return if (!matcher.matches()) null else Attribute(matcher.group(1)!!, matcher.group(2)!!)
        }

        fun split(value: CharSequence): Array<String> {
            return LIST_SEPARATOR.split(value)
        }
    }
}
