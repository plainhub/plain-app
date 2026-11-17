package com.ismartcoding.plain.features.files

internal enum class FileAction(val queryValue: String) {
    PREVIEW("PREVIEW"),
    DOWNLOAD("DOWNLOAD"),
    PROBE("PROBE"),
    TRANSCODE("TRANSCODE"),
}
