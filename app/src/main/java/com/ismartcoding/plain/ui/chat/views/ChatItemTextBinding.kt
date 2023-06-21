package com.ismartcoding.plain.ui.chat.views

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.setTextWithLinkSupport
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ChatItemTextBinding
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.features.ChatItemClickEvent
import com.ismartcoding.plain.ui.helpers.WebHelper

fun ChatItemTextBinding.initView(chatItem: DChat) {
    text.run {
        // https://stackoverflow.com/questions/15836306/can-a-textview-be-selectable-and-contain-links
        setTextColor(this.context.getColor(R.color.primary))
        setLinkTextColor(this.context.getColor(R.color.purple_light))
        setTextWithLinkSupport((chatItem.content.value as DMessageText).text) {
            WebHelper.open(context, it)
        }
        setOnClickListener {
            sendEvent(ChatItemClickEvent())
        }
        // hashtagColor = context.getColor(R.color.color_blue)
        // isHashtagEnabled = true
    }
}
