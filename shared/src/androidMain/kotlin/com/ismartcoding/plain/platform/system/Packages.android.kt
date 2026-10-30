package com.ismartcoding.plain.platform.system

import android.graphics.drawable.Drawable
import com.ismartcoding.plain.models.Package as AppPackage

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.packageManager
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import java.io.File

internal data class PackageInstallResult(
    val packageName: String,
    val lastUpdateTime: Instant?,
    val isNew: Boolean,
)

@Serializable
internal data class PackageInstallProfile(val densityDpi: Int, val localeTags: List<String>)

internal suspend fun installedPackageFacts(): List<AppPackage> = withIO { PackageHelper.installedFacts() }

internal suspend fun getPackageInfoMap(ids: List<String>): Map<String, AppPackage?> = withIO {
    PackageHelper.getPackageInfoMap(ids).mapValues { (_, packageInfo) ->
        packageInfo?.let { runCatching { PackageHelper.getPackage(it.packageName) }.getOrNull() }
    }
}

internal fun uninstallPackage(id: String) {
    PackageHelper.uninstall(appContext, id)
}

internal fun packageInstallProfile(): PackageInstallProfile {
    val configuration = appContext.resources.configuration
    return PackageInstallProfile(configuration.densityDpi, configuration.locales.toLanguageTags().split(','))
}

internal fun installPackage(paths: List<String>, packageName: String, session: Boolean): PackageInstallResult {
    require(paths.isNotEmpty())
    val files = paths.map { File(it) }
    require(files.all { it.isFile }) { "APK file not found" }
    val installed = runCatching { packageManager.getPackageInfo(packageName, 0) }.getOrNull()
    if (session) PackageInstallSession.write(files) else {
        require(files.size == 1)
        PackageHelper.install(appContext, files.single())
    }
    return PackageInstallResult(packageName, installed?.let { Instant.fromEpochMilliseconds(it.lastUpdateTime) }, installed == null)
}

internal fun getApplicationIcon(packageId: String): Drawable? {
    return try {
        val appInfo = packageManager.getApplicationInfo(packageId, 0)
        packageManager.getApplicationIcon(appInfo)
    } catch (e: Exception) {
        LogCat.d(e.toString())
        null
    }
}

internal actual suspend fun getPackageDetail(id: String): PackageDetails? = withIO {
    runCatching { PackageHelper.getPackageDetail(id) }.getOrNull()
}

internal fun isPackageInstalled(id: String): Boolean = PackageHelper.isInstalled(id)

internal actual fun canLaunchPackage(id: String): Boolean = PackageHelper.canLaunch(id)

internal actual fun launchPackage(id: String) {
    PackageHelper.launch(appContext, id)
}

internal actual fun viewPackageInSettings(id: String) {
    PackageHelper.viewInSettings(appContext, id)
}
