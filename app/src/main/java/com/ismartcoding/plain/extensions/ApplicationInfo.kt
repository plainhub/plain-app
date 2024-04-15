package com.ismartcoding.plain.extensions

import android.content.pm.ApplicationInfo

fun ApplicationInfo.isSystemApp(): Boolean {
    return flags and ApplicationInfo.FLAG_SYSTEM != 0
}

fun ApplicationInfo.isStopped(): Boolean {
    return flags and ApplicationInfo.FLAG_STOPPED != 0
}

fun ApplicationInfo.isTestOnly(): Boolean {
    return flags and ApplicationInfo.FLAG_TEST_ONLY != 0
}

fun ApplicationInfo.isSuspended(): Boolean {
    return flags and ApplicationInfo.FLAG_SUSPENDED != 0
}