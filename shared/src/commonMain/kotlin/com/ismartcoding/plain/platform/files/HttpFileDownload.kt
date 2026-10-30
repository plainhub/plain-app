package com.ismartcoding.plain.platform.files

internal expect suspend fun downloadHttpFile(
    url: String,
    name: String,
    tlsCertificateSha256: String?,
    onProgress: (Long, Long?) -> Unit,
): String
