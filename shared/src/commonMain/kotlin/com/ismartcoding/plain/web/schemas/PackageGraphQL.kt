package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.countPackages
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.platform.getPackageInfoMap
import com.ismartcoding.plain.platform.installPackage
import com.ismartcoding.plain.platform.searchPackages
import com.ismartcoding.plain.platform.uninstallPackage
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.PackageInstallPending
import com.ismartcoding.plain.web.models.PackageStatus
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addPackageSchema() {
    query("packages") {
        resolver("offset", "limit", "query", "sortBy") { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
            searchPackages(query, limit, offset, sortBy).map { it.toModel() }
        }
    }
    query("packageStatuses") {
        resolver("ids") { ids: List<ID> ->
            checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
            getPackageInfoMap(ids.map { it.value }).map {
                val pkg = it.value
                PackageStatus(ID(it.key), pkg != null, pkg?.updatedAt)
            }
        }
    }
    query("packageCount") {
        resolver("query") { query: String ->
            if (Permission.QUERY_ALL_PACKAGES.enabledAndIsGrantedAsync()) {
                countPackages(query)
            } else {
                0
            }
        }
    }
    mutation("uninstallPackages") {
        resolver("ids") { ids: List<ID> ->
            checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
            ids.forEach { uninstallPackage(it.value) }
            true
        }
    }
    mutation("installPackage") {
        resolver("path") { path: String ->
            checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
            try {
                val result = installPackage(path)
                PackageInstallPending(result.packageName, result.lastUpdateTime, result.isNew)
            } catch (e: Exception) {
                LogCat.e("Installation failed: ${e.message}", e)
                throw GraphQLError("Installation failed: ${e.message}")
            }
        }
    }
}
