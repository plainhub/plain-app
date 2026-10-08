package com.ismartcoding.plain.features.dlna

import kotlinx.serialization.Serializable

@Serializable
data class DlnaRulesSnapshot(val allowed: List<DlnaSenderRule>, val denied: List<DlnaSenderRule>)
