package com.ismartcoding.plain.platform

/**
 * Snapshot of Wi-Fi Aware capabilities, permissions, and runtime status.
 *
 * On platforms without Wi-Fi Aware (e.g. iOS) every nullable field is null
 * and every boolean is false.
 */
data class AwareDebugInfo(
    // ---- Wi-Fi Aware Support ----
    val supported: Boolean = false,
    val available: Boolean = false,
    /** Wi-Fi Aware 4.0 pairing support (Android T+). */
    val pairingSupported: Boolean = false,
    val supportedPairingCipherSuites: List<String> = emptyList(),
    val supportedDataCipherSuites: List<String> = emptyList(),

    // ---- Wi-Fi Aware Characteristics ----
    val maxServiceNameLength: Int? = null,
    val maxServiceSpecificInfoLength: Int? = null,
    val maxMatchFilterLength: Int? = null,
    /** Only populated on Android U+ (API 34+) where the API exists. */
    val maxExtendedServiceSpecificInfoLength: Int? = null,

    // ---- Permissions ----
    val nearbyDevicesPermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val wifiEnabled: Boolean = false,

    // ---- Runtime Status ----
    val attachStatus: String = "",
    val publishSessionStatus: String = "",
    val subscribeSessionStatus: String = "",
    val discoveredPeerCount: Int = 0,
)

/** Returns a fresh snapshot of Wi-Fi Aware debug info. */
expect fun getAwareDebugInfo(): AwareDebugInfo
