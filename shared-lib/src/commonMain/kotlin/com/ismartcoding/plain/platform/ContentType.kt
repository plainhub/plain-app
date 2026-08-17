package com.ismartcoding.plain.platform

/**
 * Returns the MIME type of a file path based on a platform content resolver or
 * extension lookup. May return null when the type cannot be determined.
 */
expect fun getContentTypeForPath(path: String): String?