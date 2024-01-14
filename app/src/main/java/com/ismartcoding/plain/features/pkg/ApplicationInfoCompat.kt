package com.ismartcoding.plain.features.pkg

import android.content.pm.ApplicationInfo


object ApplicationInfoCompat {
    fun isSystemApp(info: ApplicationInfo): Boolean {
        return info.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    fun isStopped(info: ApplicationInfo): Boolean {
        return info.flags and ApplicationInfo.FLAG_STOPPED != 0
    }

    fun isTestOnly(info: ApplicationInfo): Boolean {
        return info.flags and ApplicationInfo.FLAG_TEST_ONLY != 0
    }

    fun isSuspended(info: ApplicationInfo): Boolean {
        return info.flags and ApplicationInfo.FLAG_SUSPENDED != 0
    }
}

