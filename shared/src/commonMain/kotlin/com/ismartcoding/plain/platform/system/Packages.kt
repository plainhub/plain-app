package com.ismartcoding.plain.platform.system

internal expect suspend fun getPackageDetail(id: String): PackageDetails?
internal expect fun canLaunchPackage(id: String): Boolean
internal expect fun launchPackage(id: String)
internal expect fun viewPackageInSettings(id: String)
