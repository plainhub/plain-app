package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
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
import com.ismartcoding.plain.web.models.Package
import com.ismartcoding.plain.web.models.PackageInstallPending
import com.ismartcoding.plain.web.models.PackageStatus
import com.ismartcoding.plain.web.models.toModel

@GraphQLQuery
suspend fun packages(offset: Int, limit: Int, query: String, sortBy: FileSortBy): List<Package> {
    checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
    return searchPackages(query, limit, offset, sortBy).map { it.toModel() }
}

@GraphQLQuery
suspend fun packageStatuses(ids: List<ID>): List<PackageStatus> {
    checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
    return getPackageInfoMap(ids.map { it.value }).map {
        val pkg = it.value
        PackageStatus(ID(it.key), pkg != null, pkg?.updatedAt)
    }
}

@GraphQLQuery
suspend fun packageCount(query: String): Int {
    return if (Permission.QUERY_ALL_PACKAGES.enabledAndIsGrantedAsync()) {
        countPackages(query)
    } else {
        0
    }
}

@GraphQLMutation
suspend fun uninstallPackages(ids: List<ID>): Boolean {
    checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
    ids.forEach { uninstallPackage(it.value) }
    return true
}

@GraphQLMutation
suspend fun installPackage(path: String): PackageInstallPending {
    checkEnabledAsync(setOf(Permission.QUERY_ALL_PACKAGES))
    try {
        val result = com.ismartcoding.plain.platform.installPackage(path)
        return PackageInstallPending(result.packageName, result.lastUpdateTime, result.isNew)
    } catch (e: Exception) {
        LogCat.e("Installation failed: ${e.message}", e)
        throw GraphQLError("Installation failed: ${e.message}")
    }
}

fun SchemaBuilder.addPackageSchema() {
}
