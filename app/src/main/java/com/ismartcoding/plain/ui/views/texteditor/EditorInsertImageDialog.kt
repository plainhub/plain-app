package com.ismartcoding.plain.ui.views.texteditor

import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.databinding.DialogEditorInsertImageBinding
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import java.io.File

class EditorInsertImageDialog : BaseBottomSheetDialog<DialogEditorInsertImageBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.browse.setSafeClick {
            sendEvent(PickFileEvent(PickFileTag.EDITOR, PickFileType.IMAGE, multiple = false))
        }
        receiveEvent<PickFileResultEvent> { event ->
            if (event.tag != PickFileTag.EDITOR) {
                return@receiveEvent
            }
            val uri = event.uris.first()
            val context = requireContext()
            context.contentResolver.query(uri, null, null, null, null)
                ?.use { cursor ->
                    cursor.moveToFirst()
                    val cache = mutableMapOf<String, Int>()
                    val fileName = cursor.getStringValue(OpenableColumns.DISPLAY_NAME, cache)
                    cursor.close()
                    try {
                        val dir = Environment.DIRECTORY_PICTURES
                        val dst = context.getExternalFilesDir(dir)!!.path + "/$fileName"
                        val dstFile = File(dst)
                        val path =
                            if (dstFile.exists()) {
                                dstFile.newPath()
                            } else {
                                dst
                            }
                        FileHelper.copyFile(context, uri, path)
                        binding.url.text = "app://$dir/${path.getFilenameFromPath()}"
                    } catch (ex: Exception) {
                        // the picked file could be deleted
                        ex.printStackTrace()
                    }
                }
        }

        binding.button.setSafeClick {
            sendEvent(EditorInsertImageEvent(binding.url.text, binding.description.text, binding.width.text))
            dismiss()
        }
    }
}
