package com.ismartcoding.plain.platform.network

import android.os.Build
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.permissions.Permission

actual fun localNetworkPermission(): Permission? =
    if (Build.VERSION.SDK_INT >= 37 && appContext.applicationInfo.targetSdkVersion >= 37) {
        Permission.ACCESS_LOCAL_NETWORK
    } else null
