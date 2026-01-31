package com.ismartcoding.plain.extensions

import com.ismartcoding.lib.helpers.FilterField

/**
 * Normalizes comparison operator/value pairs for query fields.
 *
 * Handles cases like:
 * - op ":" and value ">1MB"  -> (">", "1MB")
 * - op ":" and value "1MB"   -> ("=", "1MB")
 */
fun FilterField.normalizeComparison(defaultOp: String = "="): Pair<String, String> {
    var op = this.op
    var value = this.value.trim()

    if (op.isEmpty() || op == ":" || op == "=") {
        op = defaultOp

        val prefixes = listOf(">=", "<=", "!=", ">", "<", "=")
        for (p in prefixes) {
            if (value.startsWith(p)) {
                op = p
                value = value.removePrefix(p).trim()
                break
            }
        }
    } else {
        // Defensive: if the value redundantly includes the operator, strip it.
        if (value.startsWith(op)) {
            value = value.removePrefix(op).trim()
        }
    }

    return op to value
}
