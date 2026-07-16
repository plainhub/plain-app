package com.ismartcoding.plain.platform

import android.Manifest
import android.net.wifi.aware.Characteristics
import android.os.Build
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport
import com.ismartcoding.plain.lib.extensions.hasPermission
import com.ismartcoding.plain.wifiAwareManager

actual fun getAwareDebugInfo(): AwareDebugInfo {
    if (!isTPlus()) {
        // Wi-Fi Aware APIs we depend on require Android T (API 33+).
        // On older devices we still surface permission/Wi-Fi state for context.
        return AwareDebugInfo(
            supported = false,
            available = false,
            locationPermissionGranted = appContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            wifiEnabled = runCatching {
                @Suppress("DEPRECATION")
                com.ismartcoding.plain.wifiManager.isWifiEnabled
            }.getOrDefault(false),
        )
    }

    val characteristics = runCatching { wifiAwareManager.getCharacteristics() }.getOrNull()
    val isAvailable = runCatching { wifiAwareManager.isAvailable }.getOrDefault(false)

    // Pairing (Wi-Fi Aware 4.0) and pairing cipher suites were added in API 34.
    // Data-path cipher suites are exposed via getSupportedCipherSuites() (no "Data" prefix)
    // and have been available since API 33.
    val pairingBitmask = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        runCatching { characteristics?.supportedPairingCipherSuites ?: 0 }.getOrDefault(0)
    } else 0
    val dataBitmask = runCatching { characteristics?.supportedCipherSuites ?: 0 }.getOrDefault(0)

    val supportedPairingCipherSuites = decodePairingCipherSuiteBitmask(pairingBitmask)
    val supportedDataCipherSuites = decodeDataCipherSuiteBitmask(dataBitmask)

    // Runtime status — read directly from the live AwareSession instance.
    val session = WifiAwareTransport.awareSession
    val publish = WifiAwareTransport.publishSession
    val subscribe = WifiAwareTransport.subscribeSession
    val discoveredPeerCount = WifiAwareTransport.discoveredPeerCount

    val nearbyGranted = appContext.hasPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
    val locationGranted = appContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
        appContext.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    val wifiEnabled = runCatching {
        @Suppress("DEPRECATION")
        com.ismartcoding.plain.wifiManager.isWifiEnabled
    }.getOrDefault(false)

    return AwareDebugInfo(
        supported = isWifiAwareSupported,
        available = isAvailable,
        pairingSupported = pairingBitmask != 0,
        supportedPairingCipherSuites = supportedPairingCipherSuites,
        supportedDataCipherSuites = supportedDataCipherSuites,
        maxServiceNameLength = characteristics?.maxServiceNameLength,
        maxServiceSpecificInfoLength = characteristics?.maxServiceSpecificInfoLength,
        maxMatchFilterLength = characteristics?.maxMatchFilterLength,
        // Android does not expose getMaxExtendedServiceSpecificInfoLength() in the
        // public Characteristics API; leave it null.
        maxExtendedServiceSpecificInfoLength = null,
        nearbyDevicesPermissionGranted = nearbyGranted,
        locationPermissionGranted = locationGranted,
        wifiEnabled = wifiEnabled,
        attachStatus = if (session != null) "attached" else "not attached",
        publishSessionStatus = if (publish != null) "active" else "inactive",
        subscribeSessionStatus = if (subscribe != null) "active" else "inactive",
        discoveredPeerCount = discoveredPeerCount,
    )
}

/**
 * Decode the pairing cipher suite bitmask returned by
 * [Characteristics.getSupportedPairingCipherSuites] (API 34+) into human-readable names.
 *
 * Bit values are taken from the deprecated `WIFI_AWARE_CIPHER_SUITE_*` constants on
 * [android.net.wifi.aware.Characteristics], which still match the bits returned by
 * the getter.
 */
private fun decodePairingCipherSuiteBitmask(bitmask: Int): List<String> {
    if (bitmask == 0) return emptyList()
    val names = mutableListOf<String>()
    // PasN suites indicate Wi-Fi Aware 4.0 pairing support.
    if (bitmask and 0x04 != 0) names += "NCS_PK_PASN_128"
    if (bitmask and 0x08 != 0) names += "NCS_PK_PASN_256"
    if (bitmask and 0x10 != 0) names += "NCS_SK_128"
    if (bitmask and 0x20 != 0) names += "NCS_SK_256"
    if (bitmask and 0x01 != 0) names += "NCS_PK_128"
    if (bitmask and 0x02 != 0) names += "NCS_PK_256"
    if (names.isEmpty()) names += "unknown(0x${bitmask.toString(16)})"
    return names
}

/**
 * Decode the data-path cipher suite bitmask returned by
 * [Characteristics.getSupportedCipherSuites] (API 33+).
 */
private fun decodeDataCipherSuiteBitmask(bitmask: Int): List<String> {
    if (bitmask == 0) return emptyList()
    val names = mutableListOf<String>()
    if (bitmask and 0x04 != 0) names += "NCS_PK_PASN_128"
    if (bitmask and 0x08 != 0) names += "NCS_PK_PASN_256"
    if (bitmask and 0x10 != 0) names += "NCS_SK_128"
    if (bitmask and 0x20 != 0) names += "NCS_SK_256"
    if (bitmask and 0x01 != 0) names += "NCS_PK_128"
    if (bitmask and 0x02 != 0) names += "NCS_PK_256"
    if (names.isEmpty()) names += "unknown(0x${bitmask.toString(16)})"
    return names
}
