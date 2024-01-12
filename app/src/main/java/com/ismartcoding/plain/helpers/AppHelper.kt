package com.ismartcoding.plain.helpers

import android.content.Context
import android.content.Intent
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.data.enums.AppChannelType
import com.ismartcoding.plain.data.enums.AppFeatureType

object AppHelper {
    fun relaunch(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}
