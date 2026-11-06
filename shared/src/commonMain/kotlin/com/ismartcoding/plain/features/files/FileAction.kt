package com.ismartcoding.plain.features.files

internal enum class FileAction(val queryValue: String) {
    PREVIEW("preview"),
    DOWNLOAD("download"),
    PROBE("probe"),
    TRANSCODE("transcode"),
}
