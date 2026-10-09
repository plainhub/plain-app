package com.ismartcoding.plain.db

import kotlin.time.Instant

data class DArchivedConversation(
    val conversationId: String,
    val conversationDate: Instant, // snapshot moment when archived; messages before this date are archived
)
