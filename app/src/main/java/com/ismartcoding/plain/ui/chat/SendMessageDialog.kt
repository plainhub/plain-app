package com.ismartcoding.plain.ui.chat

import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.softinput.hideSoftInput
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.databinding.DialogSendMessageBinding
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.features.ChatInputEditEvent
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.SendMessageEvent
import com.ismartcoding.plain.features.chat.ChatCommandType
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.views.richtext.CommandAdapter
import com.ismartcoding.plain.ui.views.richtext.Suggestion
import java.io.File

class SendMessageDialog() : BaseBottomSheetDialog<DialogSendMessageBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        receiveEvent<ChatInputEditEvent> {
            binding.input.setText(it.content)
            if (!binding.input.isFocused) {
                binding.input.requestFocus()
            }
        }
        receiveEvent<SendMessageEvent> {
            dismiss()
        }

        receiveEvent<PickFileResultEvent> { event ->
            if (event.tag != PickFileTag.SEND_MESSAGE) {
                return@receiveEvent
            }
            val items = mutableListOf<DMessageFile>()
            DialogHelper.showLoading()
            withIO {
                event.uris.forEach { uri ->
                    val context = requireContext()
                    context.contentResolver.query(uri, null, null, null, null)
                        ?.use { cursor ->
                            cursor.moveToFirst()
                            var fileName = cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
                            val size = cursor.getLongValue(OpenableColumns.SIZE)
                            val type = context.contentResolver.getType(uri) ?: ""
                            var extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
                            if (extension.isNullOrEmpty()) {
                                extension = fileName.getFilenameExtension()
                            }
                            if (extension.isNotEmpty()) {
                                fileName = fileName.getFilenameWithoutExtension() + "." + extension
                            }
                            cursor.close()
                            try {
                                val dir = when {
                                    fileName.isVideoFast() -> {
                                        Environment.DIRECTORY_MOVIES
                                    }
                                    fileName.isImageFast() -> {
                                        Environment.DIRECTORY_PICTURES
                                    }
                                    fileName.isAudioFast() -> {
                                        Environment.DIRECTORY_MUSIC
                                    }
                                    else -> {
                                        Environment.DIRECTORY_DOCUMENTS
                                    }
                                }
                                val dst = context.getExternalFilesDir(dir)!!.path + "/$fileName"
                                val dstFile = File(dst)
                                if (dstFile.exists()) {
                                    FileHelper.copyFile(context, uri, dstFile.newPath())
                                } else {
                                    FileHelper.copyFile(context, uri, dst)
                                }
                                items.add(DMessageFile(dst, size, dstFile.getDuration(context)))
                            } catch (ex: Exception) {
                                // the picked file could be deleted
                                ex.printStackTrace()
                            }
                        }
                }
                sendEvent(
                    SendMessageEvent(
                        if (event.type == PickFileType.IMAGE_VIDEO) DMessageContent(DMessageType.IMAGES.value, DMessageImages(items)) else DMessageContent(
                            DMessageType.FILES.value,
                            DMessageFiles(items)
                        )
                    )
                )
            }
        }

        binding.send.setSafeClick {
            val content = binding.input.text.toString()
            if (content.isEmpty()) {
                return@setSafeClick
            }
            sendEvent(SendMessageEvent(DMessageContent(DMessageType.TEXT.value, DMessageText(content))))
        }

        binding.cmd.setSafeClick {
            if (binding.input.isFocused) {
                binding.input.hideSoftInput()
            }
            CommandsDialog().show()
        }

        binding.files.setSafeClick {
            sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, multiple = true))
        }

        binding.images.setSafeClick {
            sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.IMAGE_VIDEO, multiple = true))
        }

        binding.input.commandAdapter = CommandAdapter(requireContext()).apply {
            addAll(ChatCommandType.values().map { Suggestion(it.value) })
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // fix the glitch bug
            if (isActive) {
                setWindowSoftInput(binding.buttons)
            }
        }, 200)
    }
}