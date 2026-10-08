package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class FileResourceRequest(val operation: String, val path: String, val params: Params = Params()) {
    @Serializable
    data class Params(val fileName: String = "", val output: String = "")
}
