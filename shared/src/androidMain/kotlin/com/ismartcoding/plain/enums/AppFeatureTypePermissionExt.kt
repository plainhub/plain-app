package com.ismartcoding.plain.enums

import com.ismartcoding.plain.features.allGranted

fun AppFeatureType.hasPermission(): Boolean {
    val p = getPermission()
    if (p != null) {
        return allGranted(p.permissions)
    }

    return true
}
