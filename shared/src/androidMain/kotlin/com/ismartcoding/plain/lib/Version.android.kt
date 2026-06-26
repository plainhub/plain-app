package com.ismartcoding.plain.lib

import android.os.Build

actual fun isP(): Boolean = Build.VERSION.SDK_INT == Build.VERSION_CODES.P
actual fun isQPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
actual fun isRPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
actual fun isSPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
actual fun isSV2Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2
actual fun isTPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
actual fun isUPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
actual fun isVPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
actual fun isBPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
