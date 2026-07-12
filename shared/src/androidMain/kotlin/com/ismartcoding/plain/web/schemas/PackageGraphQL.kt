package com.ismartcoding.plain.web.schemas
import com.ismartcoding.plain.appContext

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.apk.ApkParsers
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.PackageInstallPending
import com.ismartcoding.plain.web.models.PackageStatus
import com.ismartcoding.plain.web.models.toModel
import kotlin.time.Instant
import java.io.File

fun SchemaBuilder.addPackageSchema() {
    query("packages") {
        resolver("offset", "limit", "query", "sortBy") { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
            PackageHelper.searchAsync(query, limit, offset, sortBy).map { it.toModel() }
        }
    }
    query("packageStatuses") {
        resolver("ids") { ids: List<ID> ->
            checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
            PackageHelper.getPackageInfoMap(ids.map { it.value }).map {
                val pkg = it.value
                val updatedAt = if (pkg != null) Instant.fromEpochMilliseconds(pkg.lastUpdateTime) else null
                PackageStatus(ID(it.key), pkg != null, updatedAt)
            }
        }
    }
    query("packageCount") {
        resolver("query") { query: String ->
            if (Permission.QUERY_ALL_PACKAGES.enabledAndIsGrantedAsync()) {
                PackageHelper.count(query)
            } else {
                0
            }
        }
    }
    mutation("uninstallPackages") {
        resolver("ids") { ids: List<ID> ->
            checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
            ids.forEach {
                PackageHelper.uninstall(appContext, it.value)
            }
            true
        }
    }
    mutation("installPackage") {
        resolver("path") { path: String ->
            checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
            val file = File(path)
            if (!file.exists()) {
                throw GraphQLError("File does not exist")
            }

            try {
                val context = appContext
                if (file.name.endsWith(".apk", ignoreCase = true)) {
                    LogCat.d("Installing APK file: ${file.name}")
                    val apkMeta = ApkParsers.getMetaInfo(file)
                        ?: throw GraphQLError("Failed to parse APK package ID")

                    PackageHelper.install(context, file)
                    val packageName = apkMeta.packageName ?: ""
                    try {
                        val pkg = packageManager.getPackageInfo(packageName, 0)
                        PackageInstallPending(packageName, Instant.fromEpochMilliseconds(pkg.lastUpdateTime), isNew = false)
                    } catch (e: Exception) {
                        PackageInstallPending(packageName, null, isNew = true)
                    }
                } else {
                    throw GraphQLError("Unsupported file format. Only APK files are supported.")
                }
            } catch (e: Exception) {
                LogCat.e("Installation failed: ${e.message}", e)
                throw GraphQLError("Installation failed: ${e.message}")
            }
        }
    }
}
