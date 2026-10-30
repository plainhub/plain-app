package com.ismartcoding.plain.platform.media

import com.ismartcoding.plain.features.files.RemoteFiles
import com.ismartcoding.plain.models.ManagedResource

internal actual suspend fun prepareViewerResource(resource: ManagedResource, name: String, video: Boolean, size: Long): ViewerResource =
    if (video) ViewerResource(RemoteFiles.download(resource.url, name, resource.tlsCertificateSha256), null)
    else ViewerResource(resource.url, resource.tlsCertificateSha256)
