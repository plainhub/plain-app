package com.ismartcoding.plain.platform

import coil3.request.ImageRequest

actual fun ImageRequest.Builder.applyForceVideoDecoder(force: Boolean): ImageRequest.Builder {
    // No-op: Android's ForceVideoDecoder is a Coil Decoder Factory that uses
    // MediaMetadataRetriever to extract video frames as bitmaps. Coil on iOS
    // does not expose an equivalent Decoder plugin point — video thumbnails in
    // the media viewer are loaded via AVAssetImageGenerator at the MediaViewer
    // level instead, so this flag has no effect here.
    return this
}
