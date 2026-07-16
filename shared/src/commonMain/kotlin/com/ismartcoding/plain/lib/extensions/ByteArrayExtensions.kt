package com.ismartcoding.plain.lib.extensions

/**
 * Converts this byte array to a lowercase hexadecimal string.
 *
 * Each byte is rendered as exactly two hex digits with leading zero padding,
 * so the output length is always `this.size * 2`. Used by SHA-256 digest
 * rendering for feed entry IDs and file hash helpers.
 */
fun ByteArray.toHexString(): String =
    joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
