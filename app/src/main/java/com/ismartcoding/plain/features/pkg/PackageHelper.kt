package com.ismartcoding.plain.features.pkg

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.packageManager
import kotlinx.datetime.Instant
import java.io.File
import android.content.pm.PackageManager
import android.content.pm.Signature
import javax.security.cert.X509Certificate

object PackageHelper {
    private val appLabelCache: MutableMap<String, String> = HashMap()

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
            val signatures = signatures(pkg)
            val certs = mutableListOf<DCertificate>()
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

            val file = File(appInfo.publicSourceDir)
            val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val appType = if (isSystemApp) "system" else "user"
            if (type.isNotEmpty() && appType != type) {
                return@forEach
            }

            if (ids.isNotEmpty() && !ids.contains(appInfo.packageName)) {
                return@forEach
            }

            apps.add(
                DPackage(
                    appInfo.packageName,
                    getLabel(appInfo),
                    appType,
                    pkg.versionName ?: "",
                    appInfo.sourceDir,
                    file.length(),
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
        }
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
                        val packageInfo = packageManager.getApplicationInfo(app.packageName, 0)
                        val isSystemApp = packageInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                        val appType = if (isSystemApp) "system" else "user"
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
            appLabelCache[key] = packageManager.getApplicationLabel(packageInfo).toString()
        }

        return appLabelCache[key] ?: ""
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