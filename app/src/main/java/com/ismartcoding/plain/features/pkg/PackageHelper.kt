package com.ismartcoding.plain.features.pkg

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
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.pinyin.Pinyin
import com.ismartcoding.plain.packageManager
import kotlinx.datetime.Instant
import java.io.File
import javax.security.cert.X509Certificate

object PackageHelper {
    private val appLabelCache: MutableMap<String, String> = HashMap()
    private val appTypeCache: MutableMap<String, String> = HashMap()
    private val appCertsCache: MutableMap<String, List<DCertificate>> = HashMap()

    fun getPackageStatuses(ids: List<String>): Map<String, Boolean> {
        val packages = packageManager.getInstalledPackages(0)
        val map = mutableMapOf<String, Boolean>()
        ids.forEach { id ->
            map[id] = packages.any { it.packageName == id }
        }

        return map
    }

    fun search(query: String, limit: Int, offset: Int): List<DPackage> {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val packages = if (query.isEmpty()) packageManager.getInstalledPackages(flags).drop(offset).take(limit) else packageManager.getInstalledPackages(flags)
        val apps = mutableListOf<DPackage>()
        var type = ""
        var text = ""
        var ids = setOf<String>()
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
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

        val applications = packageManager.getInstalledApplications(0).associateBy { it.packageName }
        packages.forEach { pkg ->
            val appInfo = applications[pkg.packageName] ?: return@forEach
            var certs = appCertsCache[pkg.packageName]
            if (certs == null) {
                certs = mutableListOf<DCertificate>()
                val signatures = signatures(pkg)
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
                appCertsCache[pkg.packageName] = certs
            }

            var appType = appTypeCache[pkg.packageName]
            if (appType == null) {
                val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                appType = if (isSystemApp) "system" else "user"
                appTypeCache[pkg.packageName] = appType
            }

            if (type.isNotEmpty() && appType != type) {
                return@forEach
            }

            if (ids.isNotEmpty() && !ids.contains(appInfo.packageName)) {
                return@forEach
            }

            apps.add(
                DPackage(
                    appInfo,
                    appInfo.packageName,
                    getLabel(appInfo),
                    appType,
                    pkg.versionName ?: "",
                    appInfo.sourceDir,
                    File(appInfo.publicSourceDir).length(),
                    certs,
                    Instant.fromEpochMilliseconds(pkg.firstInstallTime),
                    Instant.fromEpochMilliseconds(pkg.lastUpdateTime),
                )
            )
        }

        return if (query.isEmpty()) {
            apps
        } else {
            apps.filter {
                text.isEmpty()
                        || it.name.contains(text, true)
                        || it.id.contains(text, true)
                        || it.certs.any { c ->
                    c.issuer.contains(text, true)
                            || c.subject.contains(text, true)
                }
            }.drop(offset).take(limit)
        }.sortedBy { Pinyin.toPinyin(it.name).lowercase() }
    }

    fun cacheAppLabels() {
        try {
            val packages = packageManager.getInstalledPackages(0)
            packages.forEach { app ->
                val packageInfo = packageManager.getApplicationInfo(app.packageName, 0)
                appLabelCache[packageInfo.packageName] = packageManager.getApplicationLabel(packageInfo).toString()
            }
        } catch (ex: Exception) {
            LogCat.d(ex.toString())
        }
    }

    fun count(query: String): Int {
        if (query.isEmpty()) {
            return packageManager.getInstalledPackages(0).count()
        } else {
            val queryGroups = SearchHelper.parse(query)
            if (queryGroups.size == 1) {
                val t = queryGroups.find { it.name == "type" }
                if (t != null) {
                    val type = t.value
                    return packageManager.getInstalledPackages(0).count { app ->
                        val key = app.packageName
                        var appType = appTypeCache[key]
                        if (appType == null) {
                            val packageInfo = packageManager.getApplicationInfo(key, 0)
                            val isSystemApp = packageInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                            appType = if (isSystemApp) "system" else "user"
                            appTypeCache[key] = type
                        }
                        appType == type
                    }
                }
            }
        }
        return search(query, Int.MAX_VALUE, 0).count()
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

    fun getLabel(context: Context, packageName: String): String {
        try {
            val pm = context.packageManager
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
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
}