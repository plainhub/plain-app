package com.ismartcoding.plain.chat

import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer

data class DReceivedChat(val chat: DChat, val peer: DPeer, val channel: DChatChannel?)
