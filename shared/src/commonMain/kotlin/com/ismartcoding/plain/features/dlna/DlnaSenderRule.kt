package com.ismartcoding.plain.features.dlna

import kotlinx.serialization.Serializable

@Serializable
data class DlnaSenderRule(val ip: String, val name: String)
