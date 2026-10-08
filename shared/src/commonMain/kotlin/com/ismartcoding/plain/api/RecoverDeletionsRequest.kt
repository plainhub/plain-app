package com.ismartcoding.plain.api

import kotlinx.serialization.Serializable

@Serializable
internal data class RecoverDeletionsRequest(val action: String = "recoverDeletions")
