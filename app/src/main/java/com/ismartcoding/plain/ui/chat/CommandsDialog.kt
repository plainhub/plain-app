package com.ismartcoding.plain.ui.chat

import android.content.Context
import android.os.Bundle
import android.view.View
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.allowSensitivePermissions
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogCommandsBinding
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.features.SendMessageEvent
import com.ismartcoding.plain.features.chat.ChatCommandType
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onSearch
import com.ismartcoding.plain.ui.models.ListItemModel


class CommandsDialog() : BaseBottomSheetDialog<DialogCommandsBinding>() {
    data class ItemModel(val cmd: ChatCommandType) : ListItemModel()

    private var searchQ: String = ""
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.run {
            initMenu(R.menu.commands)

            onSearch { q ->
                if (searchQ != q) {
                    searchQ = q
                    search()
                }
            }
        }

        binding.rv.linear().setup {
            addType<ItemModel>(R.layout.item_row)
            R.id.container.onClick {
                val m = getModel<ItemModel>()
                sendEvent(SendMessageEvent(DMessageContent(DMessageType.TEXT.value, DMessageText(":" + m.cmd.value))))
                dismiss()
            }
        }
        search()
        setWindowSoftInput(binding.topAppBar, margin = requireContext().dp2px(200))
    }

    private fun search() {
        binding.rv.models = getItems(requireContext()).filter { searchQ.isEmpty() || it.keyText.contains(searchQ, true) || it.subtitle.contains(searchQ, true) }
    }

    companion object {
        private val itemsMap = mutableMapOf<String, MutableList<ItemModel>>()
        fun getItems(context: Context): List<ItemModel> {
            val lng = LocaleHelper.currentLocale().language
            val items = itemsMap[lng] ?: mutableListOf()
            if (items.isNotEmpty()) {
                return items
            }

            val ignore = if (LocalStorage.selectedBoxId.isEmpty()) mutableSetOf(ChatCommandType.NETWORK, ChatCommandType.EDUCATION) else mutableSetOf()
            if (!context.allowSensitivePermissions()) {
                ignore.add(ChatCommandType.SOCIAL)
            }
            items.addAll(ChatCommandType.values()
                .filter { !ignore.contains(it) }
                .map {
                    ItemModel(it).apply {
                        keyText = it.getTitle()
                        subtitle = it.getDescription()
                    }
                })
            itemsMap[lng] = items

            return items
        }
    }
}