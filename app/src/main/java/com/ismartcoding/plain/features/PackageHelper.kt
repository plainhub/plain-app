package com.ismartcoding.plain.features

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.pinyin.Pinyin
import com.ismartcoding.plain.data.DCertificate
import com.ismartcoding.plain.data.DPackage
import com.ismartcoding.plain.data.DPackageDetail
import com.ismartcoding.plain.data.DPackageStub
import com.ismartcoding.plain.extensions.isSystemApp
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.packageManager
import kotlinx.datetime.Instant
import java.io.File
import javax.security.cert.X509Certificate

object PackageHelper {
    private val appLabelCache: MutableMap<String, String> = HashMap()
    private val appTypeCache: MutableMap<String, String> = HashMap()
    private val appCertsCache: MutableMap<String, List<DCertificate>> = HashMap()

    fun getPackageInfoMap(ids: List<String>): Map<String, PackageInfo?> {
        val packages = packageManager.getInstalledPackages(0).associateBy { it.packageName }
        val map = mutableMapOf<String, PackageInfo?>()
        ids.forEach { id ->
            map[id] = packages[id]
        }

        return map
    }

    fun isInstalled(packageName: String): Boolean {
        return getPackageInfoMap(listOf(packageName))[packageName] != null
    }

    fun isUninstalled(packageName: String): Boolean {
        return !isInstalled(packageName)
    }

    suspend fun searchAsync(query: String, limit: Int, offset: Int, sortBy: FileSortBy): List<DPackage> {
        try {
            var type = ""
            var text = ""
            var ids = setOf<String>()
            if (query.isNotEmpty()) {
                val queryGroups = QueryHelper.parseAsync(query)
                var t = queryGroups.find { it.name == "type" }
                if (t != null) {
                    type = t.value
                }
                t = queryGroups.find { it.name == "text" }
                if (t != null) {
                    text = t.value
                }
                t = queryGroups.find { it.name == "ids" }
                if (t != null) {
                    ids = t.value.split(",").toSet()
                }
            }

            val apps = mutableListOf<DPackageStub>()
            val appInfos = packageManager.getInstalledApplications(0)
            appInfos.forEach { appInfo ->
                if (ids.isNotEmpty() && !ids.contains(appInfo.packageName)) {
                    return@forEach
                }

                if (type.isNotEmpty()) {
                    val appType = getAppType(appInfo)
                    if (appType != type) {
                        return@forEach
                    }
                }
                apps.add(DPackageStub(appInfo, appInfo.packageName, getLabel(appInfo)))
            }

            if (query.isEmpty() || text.isEmpty()) {
                return apps.map {
                    try {
                        getPackage(it.appInfo, packageManager.getPackageInfo(it.id, PackageManager.GET_SIGNING_CERTIFICATES))
                    } catch (ex: Exception) {
                        LogCat.d(ex.toString())
                        getPackage(it.appInfo, PackageInfo())
                    }
                }.sorted(sortBy).drop(offset).take(limit)
            }

            return apps.map {
                try {
                    getPackage(it.appInfo, packageManager.getPackageInfo(it.id, PackageManager.GET_SIGNING_CERTIFICATES))
                } catch (ex: Exception) {
                    LogCat.d(ex.toString())
                    getPackage(it.appInfo, PackageInfo())
                }
            }.filter {
                text.isEmpty()
                        || it.id.contains(text, true)
                        || it.name.contains(text, true)
                        || it.certs.any { c ->
                    c.issuer.contains(text, true)
                            || c.subject.contains(text, true)
                }
            }.sorted(sortBy).drop(offset).take(limit).toList()
        } catch (ex: Exception) {
            LogCat.d(ex.toString())
            return emptyList()
        }
    }

    private fun getAppType(appInfo: ApplicationInfo): String {
        var appType = appTypeCache[appInfo.packageName]
        if (appType == null) {
            appType = if (appInfo.isSystemApp()) "system" else "user"
            appTypeCache[appInfo.packageName] = appType
        }

        return appType
    }

    fun getCerts(packageInfo: PackageInfo): List<DCertificate> {
        val packageName = packageInfo.packageName
        // why packageName could be null?
        if (packageName.isNullOrBlank()) {
            return emptyList()
        }
        val certs = appCertsCache[packageName]?.toMutableList() ?: mutableListOf()
        if (certs.isEmpty()) {
            val signatures = signatures(packageInfo)
            for (signature in signatures) {
                val cert = X509Certificate.getInstance(signature.toByteArray())
                certs.add(
                    DCertificate(
                        cert.issuerDN.name,
                        cert.subjectDN.name,
                        cert.serialNumber.toString(),
                        Instant.fromEpochMilliseconds(cert.notBefore.time),
                        Instant.fromEpochMilliseconds(cert.notAfter.time)
                    )
                )
            }
            appCertsCache[packageName] = certs
        }

        return certs
    }

    fun getPackage(packageName: String): DPackage {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val packageInfo = packageManager.getPackageInfo(packageName, flags)
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        return getPackage(appInfo, packageInfo)
    }

    private fun getPackage(appInfo: ApplicationInfo, packageInfo: PackageInfo): DPackage {
        return DPackage(
            appInfo,
            packageInfo,
            appInfo.packageName,
            getLabel(appInfo),
            getAppType(appInfo),
            packageInfo.versionName ?: "",
            appInfo.sourceDir,
            File(appInfo.publicSourceDir).length(),
            Instant.fromEpochMilliseconds(packageInfo.firstInstallTime),
            Instant.fromEpochMilliseconds(packageInfo.lastUpdateTime),
        )
    }

    fun getPackageDetail(packageName: String): DPackageDetail {
        try {
            val flags = PackageManager.GET_SIGNING_CERTIFICATES
            val packageInfo = packageManager.getPackageInfo(packageName, flags)
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return DPackageDetail(
                appInfo,
                packageInfo,
                appInfo.packageName,
                getLabel(appInfo),
                getAppType(appInfo),
                packageInfo.versionName ?: "",
                appInfo.sourceDir,
                File(appInfo.publicSourceDir).length(),
                getCerts(packageInfo),
                Instant.fromEpochMilliseconds(packageInfo.firstInstallTime),
                Instant.fromEpochMilliseconds(packageInfo.lastUpdateTime),
            )
        } catch (ex: Exception) {
            LogCat.d(ex.toString())
            return DPackageDetail(ApplicationInfo(), PackageInfo(), packageName, "", "", "", "", 0, emptyList(), Instant.DISTANT_PAST, Instant.DISTANT_PAST)
        }
    }

    fun cacheAppLabels() {
        try {
            val appInfos = packageManager.getInstalledApplications(0)
            appInfos.forEach { appInfo ->
                try {
                    appLabelCache[appInfo.packageName] = packageManager.getApplicationLabel(appInfo).toString()
                } catch (ex: Exception) {
                    LogCat.d(ex.toString())
                }
            }
        } catch (ex: Exception) {
            LogCat.d(ex.toString())
        }
    }

    suspend fun count(query: String): Int {
        if (query.isEmpty()) {
            return packageManager.getInstalledApplications(0).count()
        } else {
            val parsed = QueryHelper.parseAsync(query)
            if (parsed.size == 1) {
                val t = parsed.find { it.name == "type" }
                if (t != null) {
                    val type = t.value
                    return packageManager.getInstalledApplications(0).count { appInfo ->
                        getAppType(appInfo) == type
                    }
                }
            }
        }
        return searchAsync(query, Int.MAX_VALUE, 0, FileSortBy.SIZE_ASC).count()
    }

    private fun getLabel(packageInfo: ApplicationInfo): String {
        val key = packageInfo.packageName
        if (!appLabelCache.containsKey(key)) {
            try {
                appLabelCache[key] = packageManager.getApplicationLabel(packageInfo).toString()
            } catch (ex: Exception) {
                appLabelCache[key] = key
                LogCat.d(ex.toString())
            }
        }

        return appLabelCache[key] ?: ""
    }

    fun getLabel(packageName: String): String {
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            return getLabel(applicationInfo)
        } catch (ex: Exception) {
            LogCat.d(ex.toString())
        }

        return ""
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val res = if (drawable.intrinsicWidth > 128 || drawable.intrinsicHeight > 128) {
            Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        } else if (drawable.intrinsicWidth <= 64 || drawable.intrinsicHeight <= 64) {
            Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(res)
        drawable.setBounds(0, 0, res.width, res.height)
        drawable.draw(canvas)
        return res
    }

    fun getIcon(packageName: String): Bitmap {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val iconDrawable = packageManager.getApplicationIcon(applicationInfo)
        return drawableToBitmap(iconDrawable)
    }

    private fun signatures(packageInfo: PackageInfo): MutableSet<Signature> {
        val signatures: MutableSet<Signature> = mutableSetOf()
        val signingInfo = packageInfo.signingInfo
        if (signingInfo != null) {
            signingInfo.signingCertificateHistory?.let { signatures.addAll(it) }
            signingInfo.apkContentsSigners?.let { signatures.addAll(it) }
        }
        @Suppress("DEPRECATION")
        packageInfo.signatures?.let { signatures.addAll(it) }
        return signatures
    }

    fun uninstall(context: Context, packageName: String) {
        context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun canLaunch(packageName: String): Boolean {
        return packageManager.getLaunchIntentForPackage(packageName) != null
    }

    fun launch(context: Context, packageName: String) {
        context.startActivity(packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun viewInSettings(context: Context, packageName: String) {
        context.startActivity(Intent().apply {
            action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun install(context: Context, file: File) {
        try {
            if (!file.name.lowercase().endsWith(".apk")) {
                LogCat.e("Invalid file extension: ${file.name}")
                throw IllegalArgumentException("Invalid file extension: ${file.name}")
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                com.ismartcoding.plain.Constants.AUTHORITY,
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.applicationInfo.packageName)
            }

            context.startActivity(intent)
            LogCat.d("APK installation intent started for ${file.name}")
        } catch (e: Exception) {
            LogCat.e("Failed to install APK: ${e.message}", e)
            throw e
        }
    }

    private fun List<DPackage>.sorted(sortBy: FileSortBy): List<DPackage> {
        return when (sortBy) {
            FileSortBy.NAME_ASC -> this.sortedBy { Pinyin.toPinyin(it.name).lowercase() }
            FileSortBy.NAME_DESC -> this.sortedBy { Pinyin.toPinyin(it.name).lowercase() }
            FileSortBy.SIZE_ASC -> this.sortedBy { it.size }
            FileSortBy.SIZE_DESC -> this.sortedByDescending { it.size }
            FileSortBy.DATE_ASC -> this.sortedBy { it.updatedAt }
            FileSortBy.DATE_DESC -> this.sortedByDescending { it.updatedAt }
        }
    }
}