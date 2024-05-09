package com.ismartcoding.plain.enums

enum class ImageType {
    JPG,
    PNG,
    GIF,
    WEBP,
    WEBP_ANIMATE,
    HEIF,
    HEIF_ANIMATED,
    SVG,
    UNKNOWN;

    fun isApplicableAnimated(): Boolean {
        return setOf(GIF, WEBP_ANIMATE, HEIF_ANIMATED).contains(this)
    }
}
