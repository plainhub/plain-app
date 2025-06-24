package com.ismartcoding.plain.helpers

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.content.Context
import android.content.Intent
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.LatestRelease
import com.ismartcoding.plain.data.Version
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.preferences.CheckUpdateTimePreference
import com.ismartcoding.plain.preferences.NewVersionDownloadUrlPreference
import com.ismartcoding.plain.preferences.NewVersionLogPreference
import com.ismartcoding.plain.preferences.NewVersionPreference
import com.ismartcoding.plain.preferences.NewVersionPublishDatePreference
import com.ismartcoding.plain.preferences.NewVersionSizePreference
import com.ismartcoding.plain.preferences.SkipVersionPreference
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
            CheckUpdateTimePreference.putAsync(context, System.currentTimeMillis())
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
            val skipVersion = Version(SkipVersionPreference.getAsync(context))
            val currentVersion = Version(BuildConfig.VERSION_NAME)
            val latestVersion = Version(latest.tagName.substring(1))
            if (latestVersion.whetherNeedUpdate(currentVersion, skipVersion)) {
                NewVersionPreference.putAsync(context, latestVersion.toString())
                NewVersionLogPreference.putAsync(context, latest.body)
                NewVersionPublishDatePreference.putAsync(context, latest.publishedAt.ifEmpty { latest.createdAt })
                val apk = latest.assets.firstOrNull()
                NewVersionSizePreference.putAsync(context, apk?.size ?: 0)
                NewVersionDownloadUrlPreference.putAsync(context, apk?.browserDownloadUrl ?: "")
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
        return calculateDirectorySize(context.cacheDir) + calculateDirectorySize(context.filesDir.resolve("image_cache"))
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
