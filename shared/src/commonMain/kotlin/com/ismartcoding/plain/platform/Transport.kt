package com.ismartcoding.plain.platform


expect val isWifiAwareSupported: Boolean


/** Number of supported Wi-Fi Aware data interfaces, or null if unavailable. */
expect fun getAwareDataInterfaces(): Int?

/** Number of supported Wi-Fi Aware data paths, or null if unavailable. */
expect fun getAwareDataPaths(): Int?
