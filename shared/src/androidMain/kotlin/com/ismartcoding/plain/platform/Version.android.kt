package com.ismartcoding.plain.platform

import android.os.Build

actual fun isP(): Boolean = Build.VERSION.SDK_INT == Build.VERSION_CODES.P // android 9
actual fun isQPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q // android 10
actual fun isRPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R // android 11
actual fun isSPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // android 12
actual fun isSV2Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 // android 12L
actual fun isTPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU // android 13
actual fun isUPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
actual fun isVPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
actual fun isBPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
