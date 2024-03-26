package com.ismartcoding.plain.helpers

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.content.Context
import android.content.Intent
import com.ismartcoding.plain.MainApp

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

    fun getFileIconPath(extension: String): String {
        if (fileIcons.isEmpty()) {
            cacheIconKeys(MainApp.instance)
        }
        if (!fileIcons.contains(extension)) {
            return "file:///android_asset/ficons/default.svg"
        }

        return "file:///android_asset/ficons/$extension.svg"
    }

    fun cacheIconKeys(context: Context) {
        context.assets.list("ficons")?.forEach {
            fileIcons.add(it.substringBefore("."))
        }
    }
}
