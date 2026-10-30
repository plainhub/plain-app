package com.ismartcoding.plain.ui.platform.media

import androidx.compose.ui.unit.IntSize

expect fun tryDecodeQrCode(path: String): String?
expect fun getSvgSize(path: String): IntSize

internal expect fun tryDecodeQrCode(image: androidx.compose.ui.graphics.ImageBitmap): String?

suspend fun tryDecodeQrCode(path: String, source: com.ismartcoding.plain.rust.graphql.ContentSource): String? = when (source) {
    com.ismartcoding.plain.rust.graphql.ContentSource.Local -> tryDecodeQrCode(path)
    is com.ismartcoding.plain.rust.graphql.ContentSource.Remote -> decodeManagedImageBitmap(source.client.resource(path))?.let { tryDecodeQrCode(it) }
}
