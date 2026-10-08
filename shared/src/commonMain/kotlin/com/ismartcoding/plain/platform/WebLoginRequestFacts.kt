package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class WebLoginRequestFacts(val clientId: String, val request: com.ismartcoding.plain.features.session.AuthRequest, val requestId: String, val clientIp: String)
