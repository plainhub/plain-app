package com.ismartcoding.plain.web.models

import kotlinx.serialization.Serializable

@Serializable
data class NoteInput(
    var title: String,
    var content: String,
)
