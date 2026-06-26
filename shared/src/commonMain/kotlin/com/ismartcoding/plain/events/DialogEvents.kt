package com.ismartcoding.plain.events

import com.ismartcoding.plain.lib.channel.ChannelEvent

class ConfirmDialogEvent(
    val title: String,
    val message: String,
    val confirmButton: Pair<String, () -> Unit>,
    val dismissButton: Pair<String, () -> Unit>?
) : ChannelEvent()

class LoadingDialogEvent(
    val show: Boolean,
    val message: String = ""
) : ChannelEvent()
