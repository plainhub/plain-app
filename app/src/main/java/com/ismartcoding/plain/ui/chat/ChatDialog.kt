package com.ismartcoding.plain.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogChatBinding
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.features.ChatItemClickEvent
import com.ismartcoding.plain.features.DeleteChatItemViewEvent
import com.ismartcoding.plain.features.SendMessageEvent
import com.ismartcoding.plain.features.UpdateMessageEvent
import com.ismartcoding.plain.features.chat.ChatHelper
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.web.HttpServerEvents
import com.ismartcoding.plain.web.models.ChatItem
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import kotlinx.coroutines.launch

class ChatDialog() : BaseDialog<DialogChatBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.toolbar.run {
            title = getString(R.string.my_phone)
            onBack {
                onBackPressed()
            }
        }

        initEvents()
        binding.page.pageName = javaClass.simpleName
        binding.page.run {
//                onRefresh {
//                    finishRefresh()
//                }
            setEnableRefresh(false)
        }

        binding.chatList.initView(lifecycle)
        lifecycleScope.launch {
            binding.chatList.refreshAsync()
            binding.chatInput.initView(lifecycle)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // fix the glitch bug
            if (isActive) {
                setWindowSoftInput(binding.chatInput, editText = binding.chatInput)
            }
        }, 200)
    }

    private fun initEvents() {
        receiveEvent<ChatItemClickEvent> { event ->
            binding.chatInput.blur()
        }

        receiveEvent<DeleteChatItemViewEvent> { event ->
            binding.chatList.remove(event.id)
        }

        receiveEvent<SendMessageEvent> { event ->
            lifecycleScope.launch {
                val items = withIO { ChatHelper.createChatItemsAsync(event.content) }
                val models = mutableListOf<ChatItem>()
                items.forEach {
                    models.add(it.toModel().apply {
                        data = this.getContentData()
                    })
                }
                sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, jsonEncode(models)))
                binding.chatList.addAll(items)
                DialogHelper.hideLoading()
            }
        }

        receiveEvent<HttpServerEvents.MessageCreatedEvent> { event ->
            binding.chatList.addAll(event.items)
        }

        receiveEvent<UpdateMessageEvent> { event ->
            lifecycleScope.launch {
                val update = ChatItemDataUpdate(event.chatItem.id, event.chatItem.content)
                withIO {
                    AppDatabase.instance.chatDao().updateData(update)
                }
                binding.chatList.update(event.chatItem)
                sendEvent(WebSocketEvent(EventType.MESSAGE_UPDATED, jsonEncode(listOf(event.chatItem.toModel().apply {
                    data = this.getContentData()
                }))))
            }
        }
    }
}