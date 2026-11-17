package com.ismartcoding.plain.ui.images.editor

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.ismartcoding.plain.events.*
import com.ismartcoding.plain.features.files.*
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.files.readPickedFileBytes
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.images.editor.scene.decodeSceneImages
import com.ismartcoding.plain.ui.images.editor.scene.renderSceneBitmap
import kotlinx.coroutines.CancellationException

internal sealed interface EditorImageTarget {
    data object Background : EditorImageTarget
    data object Layer : EditorImageTarget
    data class Replacement(val id: String) : EditorImageTarget
}

internal class EditorImageActions(private val vm: ImageEditorEditViewModel) {
    private var importTarget: EditorImageTarget? = null
    private var exportImage: Pair<EditorRasterFormat, ByteArray>? = null

    fun pick(target: EditorImageTarget) {
        importTarget = target
        sendEvent(PickFileEvent(PickFileTag.IMAGE_EDITOR, PickFileType.IMAGE, false))
    }

    fun render(textMeasurer: TextMeasurer): ImageBitmap {
        val doc = vm.document.value
        return renderSceneBitmap(doc, textMeasurer, decodeSceneImages(doc.sourceImage, doc.images))
    }

    suspend fun copy(textMeasurer: TextMeasurer) {
        val path = com.ismartcoding.plain.platform.system.cacheDirPath() + "/image-editor-clipboard.png"
        val bytes = encodeEditorImage(render(textMeasurer), EditorRasterFormat.PNG)
        check(FilesClient.writeChunks(path, listOf(bytes).iterator())) { "Unable to prepare clipboard image" }
        publishEditorClipboardImage(path)
    }

    fun export(format: EditorRasterFormat, textMeasurer: TextMeasurer) {
        val image = render(textMeasurer)
        val raster = if (format == EditorRasterFormat.JPEG) ImageBitmap(image.width, image.height).also {
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(it.width.toFloat(), it.height.toFloat())) { drawRect(Color.White); drawImage(image) }
        } else image
        exportImage = format to encodeEditorImage(raster, format)
        sendEvent(ExportFileEvent(format.exportType, "image.${format.extension}"))
    }

    suspend fun receive(event: com.ismartcoding.plain.lib.ChannelEvent) {
        when {
            event is PickFileResultEvent && event.tag == PickFileTag.IMAGE_EDITOR -> {
                val target = checkNotNull(importTarget)
                importTarget = null
                val image = importEditorImage(readPickedFileBytes(event.uris.first()))
                val png = embeddedEditorPng(image)
                when (target) {
                    EditorImageTarget.Background -> vm.setBackgroundImage(png)
                    EditorImageTarget.Layer -> vm.addImage(png, image.width, image.height)
                    is EditorImageTarget.Replacement -> vm.replaceImage(target.id, png)
                }
            }
            event is ExportFileResultEvent && event.type in EditorRasterFormat.entries.map { it.exportType } -> {
                val (format, bytes) = checkNotNull(exportImage)
                check(event.type == format.exportType)
                check(FilesClient.writeChunks(event.uri, listOf(bytes).iterator())) { "Unable to write selected image file" }
                exportImage = null
            }
        }
    }
}

@Composable
internal fun ImageEditorFileEffects(actions: EditorImageActions) {
    LaunchedEffect(actions) {
        Channel.sharedFlow.collect { event ->
            try { actions.receive(event) }
            catch (canceled: CancellationException) { throw canceled }
            catch (error: Exception) { DialogHelper.showMessage(error) }
        }
    }
}
