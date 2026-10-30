package com.ismartcoding.plain.platform.media

import com.ismartcoding.plain.features.files.RemoteFiles
import com.ismartcoding.plain.models.ManagedResource

internal const val TILED_IMAGE_MIN_BYTES = 2_000_000L

internal actual suspend fun prepareViewerResource(resource: ManagedResource, name: String, video: Boolean, size: Long): ViewerResource =
    if (!video && size > TILED_IMAGE_MIN_BYTES) ViewerResource(RemoteFiles.download(resource.url, name, resource.tlsCertificateSha256), null)
    else ViewerResource(resource.url, resource.tlsCertificateSha256)
