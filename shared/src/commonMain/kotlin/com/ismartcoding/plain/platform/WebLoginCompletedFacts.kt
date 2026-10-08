package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class WebLoginCompletedFacts(val request: com.ismartcoding.plain.features.session.AuthRequest, val chatPaired: Boolean, val clientIp: String)
