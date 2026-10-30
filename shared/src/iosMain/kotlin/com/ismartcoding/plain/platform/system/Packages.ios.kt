package com.ismartcoding.plain.platform.system

internal actual suspend fun getPackageDetail(id: String): PackageDetails? =
    throw UnsupportedOperationException("unsupported capability PACKAGES")

internal actual fun canLaunchPackage(id: String): Boolean = false

internal actual fun launchPackage(id: String): Unit =
    throw UnsupportedOperationException("unsupported capability PACKAGES")

internal actual fun viewPackageInSettings(id: String): Unit =
    throw UnsupportedOperationException("unsupported capability PACKAGES")
