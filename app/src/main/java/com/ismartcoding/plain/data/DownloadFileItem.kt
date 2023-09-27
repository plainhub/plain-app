package com.ismartcoding.plain.data

import com.ismartcoding.lib.extensions.IJSONItem
import org.json.JSONObject
import java.io.File

data class DownloadFileItem(val path: String, val name: String) : IJSONItem {
    override fun toJSON(): JSONObject {
        return JSONObject()
    }
}

data class DownloadFileItemWrap(val file: File, val name: String)
