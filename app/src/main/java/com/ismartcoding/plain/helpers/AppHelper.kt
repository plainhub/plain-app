package com.ismartcoding.plain.helpers

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.LatestRelease
import com.ismartcoding.plain.data.Version
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.preferences.UpdateInfoPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import java.io.File

object AppHelper {
    private val fileIcons = mutableSetOf<String>()

    fun relaunch(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    fun foregrounded(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE)
    }

    suspend fun checkUpdateAsync(context: Context, showToast: Boolean): Boolean? {
        return try {
            val client = HttpClientManager.httpClient()
            val r = client.get(Constants.LATEST_RELEASE_URL)
            UpdateInfoPreference.updateAsync(context) { it.copy(checkUpdateTime = System.currentTimeMillis()) }
            if (r.status == HttpStatusCode.Forbidden) {
                if (showToast) {
                    DialogHelper.showMessage(getString(R.string.rate_limit))
                }
                return false
            }

            val latestJSON = r.bodyAsText()
            if (latestJSON.isEmpty()) {
                if (showToast) {
                    DialogHelper.showMessage(getString(R.string.check_failure))
                }
                return null
            }

            val latest = jsonDecode<LatestRelease>(latestJSON)
            val current = UpdateInfoPreference.getValueAsync(context)
            val skipVersion = Version(current.skipVersion)
            val currentVersion = Version(BuildConfig.VERSION_NAME)
            val latestVersion = Version(latest.tagName.substring(1))
            if (latestVersion.whetherNeedUpdate(currentVersion, skipVersion)) {
                val only32BitDevice = Build.SUPPORTED_64_BIT_ABIS.isEmpty()
                val apk = if (only32BitDevice) {
                    latest.assets.firstOrNull {
                        it.name.endsWith("-Old-Android-32bit.apk")
                    }
                } else {
                    latest.assets.firstOrNull {
                        it.name.endsWith("-Universal-Recommended.apk") || it.name.endsWith(
                            "-default.apk"
                        )
                    }
                }
                UpdateInfoPreference.updateAsync(context) {
                    it.copy(
                        newVersion = latestVersion.toString(),
                        log = latest.body,
                        publishDate = latest.publishedAt.ifEmpty { latest.createdAt },
                        size = apk?.size ?: 0,
                        downloadUrl = apk?.browserDownloadUrl ?: "",
                    )
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (showToast) {
                DialogHelper.showMessage(getString(R.string.check_failure))
            }
            null
        }
    }

    fun getCacheSize(context: Context): Long {
        return calculateDirectorySize(context.cacheDir) + calculateDirectorySize(
            context.filesDir.resolve(
                "image_cache"
            )
        )
    }

    private fun calculateDirectorySize(directory: File): Long {
        var totalSize: Long = 0
        val files = directory.listFiles() ?: return 0L
        for (file in files) {
            totalSize += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }

        return totalSize
    }

    fun clearCacheAsync(context: Context) {
        context.cacheDir.listFiles()?.forEach {
            it.deleteRecursively()
        }
        context.filesDir.resolve("image_cache").listFiles()?.forEach {
            it.deleteRecursively()
        }
        context.filesDir.resolve("upload_tmp").listFiles()?.forEach {
            it.deleteRecursively()
        }
    }

    fun getFileIconPath(extension: String): String {
        if (fileIcons.isEmpty()) {
            cacheIconKeys(MainApp.instance)
        }
        if (!fileIcons.contains(extension)) {
            return "file:///android_asset/ficons/default.svg"
        }

        return "file:///android_asset/ficons/$extension.svg"
    }

    private fun cacheIconKeys(context: Context) {
        context.assets.list("ficons")?.forEach {
            fileIcons.add(it.substringBefore("."))
        }
    }
}
