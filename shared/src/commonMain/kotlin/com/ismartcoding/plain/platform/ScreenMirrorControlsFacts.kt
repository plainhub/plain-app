package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class ScreenMirrorControlsFacts(val inputs: List<com.ismartcoding.plain.data.ScreenMirrorControlInput>)
