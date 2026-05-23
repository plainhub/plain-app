package com.ismartcoding.plain.ui.page.home.chat

import org.jetbrains.compose.resources.DrawableResource
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.ui.nav.Routing
import kotlin.time.Instant

data class ChatRow(
    val sortAt: Instant,
    val title: String,
    val desc: String,
    val icon: DrawableResource,
    val online: Boolean?,
    val createdAt: Instant,
    val latestChat: DChat?,
    val route: Routing.Chat,
)