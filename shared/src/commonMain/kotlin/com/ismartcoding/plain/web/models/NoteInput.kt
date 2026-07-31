package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput
import kotlinx.serialization.Serializable

@GraphQLInput
@Serializable
data class NoteInput(
    var title: String,
    var content: String,
)
