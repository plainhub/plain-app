package com.ismartcoding.plain.platform

enum class NetworkType { WIFI, CELLULAR, ETHERNET, NONE }

/**
 * Current primary network type.
 */
expect fun getNetworkType(): NetworkType