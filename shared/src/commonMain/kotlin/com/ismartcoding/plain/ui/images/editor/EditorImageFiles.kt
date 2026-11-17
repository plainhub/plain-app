package com.ismartcoding.plain.ui.images.editor

import androidx.compose.ui.graphics.ImageBitmap
import com.ismartcoding.plain.features.files.ExportFileType
import com.ismartcoding.plain.helpers.Base64Lenient

enum class EditorRasterFormat(val extension: String, val exportType: ExportFileType) {
    PNG("png", ExportFileType.IMAGE_PNG),
    JPEG("jpg", ExportFileType.IMAGE_JPEG),
    WEBP("webp", ExportFileType.IMAGE_WEBP),
}

internal expect suspend fun importEditorImage(bytes: ByteArray): ImageBitmap
internal expect fun encodeEditorImage(image: ImageBitmap, format: EditorRasterFormat): ByteArray
internal fun embeddedEditorPng(image: ImageBitmap): String = "data:image/png;base64," + Base64Lenient.encode(encodeEditorImage(image, EditorRasterFormat.PNG))

internal expect fun publishEditorClipboardImage(path: String)
