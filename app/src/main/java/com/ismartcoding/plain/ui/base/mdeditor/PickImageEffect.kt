package com.ismartcoding.plain.ui.base.mdeditor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.appDir
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.lib.extensions.queryOpenableFileName
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.helpers.FileHelper
import java.io.File

@Composable
internal fun PickImageEffect(
    context: Context,
    imageUrl: MutableState<String>,
) {
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.EDITOR) {
                        return@collect
                    }
                    val uri = event.uris.first()
                    try {
                        val fileName = context.contentResolver.queryOpenableFileName(uri)
                        if (fileName.isNotEmpty()) {
                            val dst = context.appDir() + "/note-images/" + "/$fileName"
                            val dstFile = File(dst)
                            val path =
                                if (dstFile.exists()) {
                                    dstFile.newPath()
                                } else {
                                    dst
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
