/*
 * Path template matcher for the plain HTTP dispatcher.
 *
 * Matches a request path against templates using the `{name}` convention
 * (the same convention the commonMain HttpRouter route table uses). A
 * template segment of `{name}` matches exactly one path segment and captures
 * it; `{name...}` (tailcard) matches the rest of the path and joins it with
 * "/".
 */
package com.ismartcoding.plain.lib.ktorserver

/**
 * Returns captured parameters when [template] matches [path], or null.
 * Both inputs may carry query strings; only the path part is compared.
 */
fun matchPathTemplate(template: String, path: String): Map<String, String>? {
    val templateSegments = segment(template)
    val pathSegments = segment(path)
    var i = 0
    val params = HashMap<String, String>()

    while (i < templateSegments.size) {
        val t = templateSegments[i]
        if (t.startsWith("{") && t.endsWith("...}")) {
            // Tailcard: matches zero or more remaining segments.
            if (i != templateSegments.lastIndex) return null
            val name = t.substring(1, t.length - 4)
            if (name.isNotEmpty() && i < pathSegments.size) {
                params[name] = pathSegments.subList(i, pathSegments.size).joinToString("/")
            }
            return params
        }
        if (i >= pathSegments.size) return null
        val p = pathSegments[i]
        if (t.startsWith("{") && t.endsWith("}")) {
            params[t.substring(1, t.length - 1)] = p
        } else if (!t.equals(p, ignoreCase = false)) {
            return null
        }
        i++
    }
    // All template segments consumed; path must be fully consumed too.
    return if (i == pathSegments.size) params else null
}

private fun segment(path: String): List<String> {
    val clean = path.substringBefore('?').trimEnd('/')
    if (clean.isEmpty() || clean == "/") return emptyList()
    return clean.trimStart('/').split('/')
}
