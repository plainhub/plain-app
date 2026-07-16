package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.file.FileSortBy

actual suspend fun searchPackages(
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<DPackageInfo> = emptyList()

actual suspend fun countPackages(query: String): Int = 0

actual suspend fun getPackageInfoMap(ids: List<String>): Map<String, DPackageInfo?> =
    ids.associateWith { null }

actual fun uninstallPackage(id: String) = Unit

actual fun installPackage(path: String): PackageInstallResult =
    throw IllegalArgumentException("APK installation is not supported on iOS")

actual fun getApplicationIcon(packageId: String): Any? = null

actual suspend fun getPackageDetail(id: String): DPackageInfo? = null

actual fun isPackageInstalled(id: String): Boolean = false

actual fun canLaunchPackage(id: String): Boolean = false

actual fun launchPackage(id: String) = Unit

actual fun viewPackageInSettings(id: String) = Unit

actual fun getManifestXml(path: String): String = ""
