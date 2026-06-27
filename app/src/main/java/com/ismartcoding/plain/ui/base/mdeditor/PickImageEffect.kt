package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.lib.extensions.appDir
import com.ismartcoding.plain.lib.extensions.getFileName
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.appContext
import java.io.File

@Composable
internal fun PickImageEffect(
    imageUrl: MutableState<String>,
) {
    val context = appContext
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.EDITOR) {
                        return@collect
                    }
                    val uri = event.uris.firstOrNull() ?: return@collect
                    try {
                        val fileName = uri.getFileName(context).ifEmpty {
                            "image_${System.currentTimeMillis()}.jpg"
                        }.replace("/", "_")
                        if (fileName.isNotEmpty()) {
                            val noteImagesDir = File(context.appDir(), "note-images")
                            if (!noteImagesDir.exists()) {
                                noteImagesDir.mkdirs()
                            }
                            val dstFile = File(noteImagesDir, fileName)
                            val path =
                                if (dstFile.exists()) {
                                    dstFile.newPath()
                                } else {
                                    dstFile.absolutePath
                                }
                            FileHelper.copyFile(context, uri, path)
                            imageUrl.value = "app://note-images/${path.getFilenameFromPath()}"
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
    }
}
