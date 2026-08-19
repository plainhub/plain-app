package com.ismartcoding.plain.lib.extensions

/**
 * Detect whether [header] is the `ftyp` box of a HEIF/HEIC/AVIF container.
 * The bytes are the first 12 bytes of the file.
 */
fun isHeifHeader(header: ByteArray): Boolean {
    if (header.size < 12) return false
    return header[4] == 0x66.toByte() && // 'f'
        header[5] == 0x74.toByte() && // 't'
        header[6] == 0x79.toByte() && // 'y'
        header[7] == 0x70.toByte() && // 'p'
        header.copyOfRange(8, 12).decodeToString() in listOf("heic", "heix", "hevc", "hevx", "avif")
}

/**
 * Whether a file is an animated image (GIF, animated WebP, animated HEIF) or an
 * SVG, decided from its display [fileName] plus the first up-to-256 content bytes
 * ([header], which should be zero-padded when the file is shorter). [totalBytes]
 * is the full file size. Mirrors the magic-byte sniff of the reference
 * `ImageHelper.getImageType` (Android) so both platforms agree on the shared
 * `/fs` serve path (which must not run the HEIF→PNG conversion on such files).
 */
fun isAnimatedImageOrSvgHeader(fileName: String, header: ByteArray, totalBytes: Long): Boolean {
    val extension = fileName.getFilenameExtension()
    if (extension == "svg") return true
    if (extension == "png" || extension == "jpg" || extension == "jpeg") {
        return false
    }

    // GIF87a / GIF89a magic at offset 0.
    if (header.size >= 6) {
        val gif = header.decodeToString(0, 6)
        if (gif == "GIF87a" || gif == "GIF89a") return true
    }

    // JPEG / PNG magic bytes are never animated; stop the sniff cheaply.
    val b0 = header.getOrNull(0)?.toInt()
    val b1 = header.getOrNull(1)?.toInt()
    if (b0 == -1 && b1 == -40 || b0 == -119 && b1 == 80) return false

    // WebP: "RIFF" + "WEBP"; an "VP8X" box's animation bit is bit 1 of byte 16.
    if (header.size >= 17 &&
        header.decodeToString(0, 4) == "RIFF" &&
        header.decodeToString(8, 12) == "WEBP"
    ) {
        if (header.decodeToString(12, 16) == "VP8X" &&
            totalBytes > 17 &&
            (header[16].toInt() and 0b10) > 0
        ) {
            return true
        }
        return false
    }

    // HEIF: an "ftyp" box followed by an animated brand (msf1/hevc/hevx).
    if (header.size >= 12 && header.decodeToString(4, 8) == "ftyp") {
        val brand = header.decodeToString(8, 12)
        if (brand == "msf1" || brand == "hevc" || brand == "hevx") return true
        return false
    }

    // SVG: scan the readable window for the "<svg" tag.
    return header.decodeToString(0, minOf(header.size, 256)).contains("<svg")
}