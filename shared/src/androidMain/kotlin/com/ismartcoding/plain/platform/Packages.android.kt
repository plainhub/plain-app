package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.DCertificate
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.apk.ApkParsers
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.packageManager
import kotlin.time.Instant

actual suspend fun searchPackages(
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<DPackageInfo> = withIO {
    PackageHelper.searchAsync(query, limit, offset, sortBy).map { pkg ->
        DPackageInfo(
            id = pkg.id,
            name = pkg.name,
            type = pkg.type,
            version = pkg.version,
            path = pkg.path,
            size = pkg.size,
            installedAt = pkg.installedAt,
            updatedAt = pkg.updatedAt,
            certs = PackageHelper.getCerts(pkg.packageInfo),
        )
    }
}

actual suspend fun countPackages(query: String): Int = withIO {
    PackageHelper.count(query)
}

actual suspend fun getPackageInfoMap(ids: List<String>): Map<String, DPackageInfo?> = withIO {
    val packageInfos = PackageHelper.getPackageInfoMap(ids)
    packageInfos.mapValues { (_, pkgInfo) ->
        if (pkgInfo == null) null
        else try {
            val pkg = PackageHelper.getPackage(pkgInfo.packageName)
            DPackageInfo(
                id = pkg.id,
                name = pkg.name,
                type = pkg.type,
                version = pkg.version,
                path = pkg.path,
                size = pkg.size,
                installedAt = pkg.installedAt,
                updatedAt = pkg.updatedAt,
                certs = PackageHelper.getCerts(pkgInfo),
            )
        } catch (e: Exception) {
            LogCat.d(e.toString())
            null
        }
    }
}

actual fun uninstallPackage(id: String) {
    PackageHelper.uninstall(appContext, id)
}

actual fun installPackage(path: String): PackageInstallResult {
    val file = java.io.File(path)
    if (!file.exists()) {
        throw IllegalArgumentException("File does not exist")
    }
    if (!file.name.endsWith(".apk", ignoreCase = true)) {
        throw IllegalArgumentException("Unsupported file format. Only APK files are supported.")
    }
    LogCat.d("Installing APK file: ${file.name}")
    val apkMeta = ApkParsers.getMetaInfo(file)
        ?: throw IllegalArgumentException("Failed to parse APK package ID")

    PackageHelper.install(appContext, file)
    val packageName = apkMeta.packageName ?: ""
    return try {
        val pkg = packageManager.getPackageInfo(packageName, 0)
        PackageInstallResult(
            packageName = packageName,
            lastUpdateTime = Instant.fromEpochMilliseconds(pkg.lastUpdateTime),
            isNew = false,
        )
    } catch (e: Exception) {
        PackageInstallResult(
            packageName = packageName,
            lastUpdateTime = null,
            isNew = true,
        )
    }
}

actual fun getApplicationIcon(packageId: String): Any? {
    return try {
        val appInfo = packageManager.getApplicationInfo(packageId, 0)
        packageManager.getApplicationIcon(appInfo)
    } catch (e: Exception) {
        LogCat.d(e.toString())
        null
    }
}

actual suspend fun getPackageDetail(id: String): DPackageInfo? = withIO {
    try {
        val detail = PackageHelper.getPackageDetail(id)
        DPackageInfo(
            id = detail.id,
            name = detail.name,
            type = detail.type,
            version = detail.version,
            path = detail.path,
            size = detail.size,
            installedAt = detail.installedAt,
            updatedAt = detail.updatedAt,
            certs = detail.certs,
            sourceDir = detail.appInfo.sourceDir ?: "",
            dataDir = detail.appInfo.dataDir ?: "",
            targetSdkVersion = detail.appInfo.targetSdkVersion,
            minSdkVersion = detail.appInfo.minSdkVersion,
            versionCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(detail.packageInfo),
            hasLargeHeap = detail.hasLargeHeap,
        )
    } catch (e: Exception) {
        LogCat.d(e.toString())
        null
    }
}

actual fun isPackageInstalled(id: String): Boolean = PackageHelper.isInstalled(id)

actual fun canLaunchPackage(id: String): Boolean = PackageHelper.canLaunch(id)

actual fun launchPackage(id: String) {
    PackageHelper.launch(appContext, id)
}

actual fun viewPackageInSettings(id: String) {
    PackageHelper.viewInSettings(appContext, id)
}

actual fun getManifestXml(path: String): String {
    return try {
        ApkParsers.getManifestXml(path)
    } catch (e: Exception) {
        LogCat.d(e.toString())
        ""
    }
}
