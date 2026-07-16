package com.ismartcoding.plain.platform

import coil3.request.ImageRequest

/**
 * Apply a forced video-frame decoder to the [ImageRequest.Builder] when [force] is true.
 * On Android this installs [com.ismartcoding.plain.platform.ForceVideoDecoder.Factory];
 * on iOS it is a no-op (video thumbnails are not yet supported there).
 */
expect fun ImageRequest.Builder.applyForceVideoDecoder(force: Boolean): ImageRequest.Builder
