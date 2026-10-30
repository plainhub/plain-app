package com.ismartcoding.plain.platform.media

import com.ismartcoding.plain.models.ManagedResource

internal data class ViewerResource(val path: String, val tlsCertificateSha256: String?)
internal expect suspend fun prepareViewerResource(resource: ManagedResource, name: String, video: Boolean, size: Long): ViewerResource
