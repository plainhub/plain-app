package com.ismartcoding.plain.ui.page.home.chat

import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.ui.nav.Routing
import kotlin.time.Instant

data class ChatRow(
    val sortAt: Instant,
    val title: String,
    val desc: String,
    val icon: Int,
    val online: Boolean?,
    val latestChat: DChat?,
    val route: Routing.Chat,
)