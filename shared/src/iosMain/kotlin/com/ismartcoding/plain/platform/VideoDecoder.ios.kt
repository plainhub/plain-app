package com.ismartcoding.plain.platform

import coil3.request.ImageRequest

actual fun ImageRequest.Builder.applyForceVideoDecoder(force: Boolean): ImageRequest.Builder {
    // iOS stub — video thumbnail decoding is not yet supported.
    return this
}
