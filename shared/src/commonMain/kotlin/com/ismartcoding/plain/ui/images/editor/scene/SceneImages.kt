package com.ismartcoding.plain.ui.images.editor.scene

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import com.ismartcoding.plain.helpers.Base64Lenient

internal data class SceneImages(val source: ImageBitmap?, val layers: Map<String, ImageBitmap>)

internal fun decodeSceneImages(source: String?, layers: Map<String, String>): SceneImages =
    SceneImages(source?.let(::decodeSceneImage), layers.mapValues { decodeSceneImage(it.value) })

internal fun decodeSceneImage(data: String): ImageBitmap {
    require(data.startsWith("data:image/png;base64,")) { "Expected an embedded editor PNG image" }
    return Base64Lenient.decode(data.substringAfter(',')).decodeToImageBitmap()
}
