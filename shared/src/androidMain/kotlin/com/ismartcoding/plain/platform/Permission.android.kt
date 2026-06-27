package com.ismartcoding.plain.platform

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.appContextValue

actual fun isPermissionGranted(perm: String): Boolean {
    val ctx = appContextValue ?: return false
    return ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
}