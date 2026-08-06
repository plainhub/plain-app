package com.ismartcoding.plain.lib.markdown.model

/**
 * This enum controls how image width is calculated for images within the markdown renderer.
 * For inline images, the size is reported as an inline placeholder size.
 * See [ImageTransformer.placeholderConfig]
 * */
enum class ImageWidth {
    IMAGE_WIDTH,
    MAX_WIDTH,
}
