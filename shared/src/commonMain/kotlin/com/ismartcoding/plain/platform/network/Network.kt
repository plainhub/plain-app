package com.ismartcoding.plain.platform.network

import kotlinx.serialization.Serializable

@Serializable
enum class NetworkType { WIFI, CELLULAR, ETHERNET, NONE, OTHER }

/**
 * Current primary network type.
 */
expect fun getNetworkType(): NetworkType

internal expect fun vpnActive(): Boolean
