package com.ismartcoding.plain.ui.chat.views


import android.content.Context
import android.os.Environment
import android.provider.OpenableColumns
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.MimeTypeMap
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.softinput.hideSoftInput
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.databinding.ViewChatInputBinding
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.SendMessageEvent
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.views.CustomViewBase
import com.ismartcoding.plain.ui.views.richtext.CommandAdapter
import java.io.File

class ChatInputView(context: Context, attrs: AttributeSet? = null) : CustomViewBase(context, attrs) {
    private val binding = ViewChatInputBinding.inflate(LayoutInflater.from(context), this, true)

    fun blur() {
        if (binding.input.isFocused) {
            binding.input.hideSoftInput()
            binding.input.clearFocus()
        }
    }

    override fun hasFocus(): Boolean {
        return binding.input.hasFocus()
    }

    fun focus() {
        if (!binding.input.isFocused) {
            binding.input.requestFocus()
        }
    }

    fun setValue(value: String) {
        binding.input.setText(value)
    }

    fun initView(lifecycle: Lifecycle) {
        registerLifecycleOwner(lifecycle)

        events.add(receiveEventHandler<PickFileResultEvent> { event ->
            if (event.tag != PickFileTag.SEND_MESSAGE) {
                return@receiveEventHandler
            }
            val items = mutableListOf<DMessageFile>()
            DialogHelper.showLoading()
            withIO {
                event.uris.forEach { uri ->
                    context.contentResolver.query(uri, null, null, null, null)
                        ?.use { cursor ->
                            cursor.moveToFirst()
                            val fileName = cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
                            val size = cursor.getLongValue(OpenableColumns.SIZE)
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
        })

        binding.send.setSafeClick {
            val content = binding.input.text.toString()
            if (content.isEmpty()) {
                return@setSafeClick
            }
            sendEvent(SendMessageEvent(DMessageContent(DMessageType.TEXT.value, DMessageText(content))))
            binding.input.setText("")
        }

        binding.input.setOnFocusChangeListener { _, hasFocus ->
            binding.buttons.isVisible = hasFocus
        }

        binding.files.setSafeClick {
            sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, multiple = true))
        }

        binding.images.setSafeClick {
            sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.IMAGE_VIDEO, multiple = true))
        }

        binding.input.commandAdapter = CommandAdapter(context).apply {
            //      addAll(HomeItemType.values().map { Suggestion(it.value) })
        }
    }
}