package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.pinyin.Pinyin
import com.ismartcoding.plain.platform.installedPackageFacts
import kotlinx.serialization.json.*

internal object SystemPackagesHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemPackageFacts" -> JsonHelper.jsonEncodeToElement(installedPackageFacts().map { item ->
            PackageFacts(
                item = item,
                nameSortKey = Pinyin.toPinyin(item.name).lowercase(),
            )
        })
        "systemPackageStatuses" -> {
            val ids = params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }
            JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getPackageInfoMap(ids))
        }
        "systemInstallPackage" -> {
            val path = params.getValue("path").jsonPrimitive.content
            val result = try {
                com.ismartcoding.plain.platform.installPackage(path)
            } catch (e: Exception) {
                throw IllegalStateException("Installation failed: ${e.message}", e)
            }
            JsonHelper.jsonEncodeToElement(PackageInstallFacts(
                packageName = result.packageName,
                lastUpdateTime = result.lastUpdateTime?.toString(),
                isNew = result.isNew,
            ))
        }
        "systemUninstallPackages" -> {
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.forEach {
                com.ismartcoding.plain.platform.uninstallPackage(it)
            }
            JsonHelper.jsonEncodeToElement(true)
        }
        else -> error("Unsupported provider operation")
    }
}
