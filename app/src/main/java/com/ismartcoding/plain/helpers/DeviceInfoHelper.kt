package com.ismartcoding.plain.helpers

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.activityManager
import com.ismartcoding.plain.data.DAndroidDeviceInfo
import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.data.DDisplayInfo
import com.ismartcoding.plain.data.DevicePlatform
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.subscriptionManager
import com.ismartcoding.plain.telephonyManager
import kotlin.time.Instant


object DeviceInfoHelper {
    @SuppressLint("HardwareIds")
    fun getDeviceInfo(context: Context): DDeviceInfo {
        val info = DDeviceInfo()
        info.name = PhoneHelper.getDeviceName(context)
        info.platform = DevicePlatform.ANDROID
        info.manufacturer = Build.MANUFACTURER
        info.model = Build.MODEL
        info.osName = "Android"
        info.osVersion = Build.VERSION.RELEASE
        info.kernelVersion = System.getProperty("os.version") ?: ""
        info.appVersion = BuildConfig.VERSION_NAME
        info.appBuildNumber = BuildConfig.VERSION_CODE.toString()
        info.language = java.util.Locale.getDefault().language
        info.uptime = SystemClock.elapsedRealtime()
        info.cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: ""

        val mi = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(mi)
        info.totalMemory = mi.totalMem

        val stat = StatFs(Environment.getDataDirectory().path)
        info.totalStorage = stat.totalBytes

        val dm = context.resources.displayMetrics
        val displayInfo = DDisplayInfo()
        displayInfo.width = dm.widthPixels
        displayInfo.height = dm.heightPixels
        displayInfo.density = dm.density.toString()
        info.display = displayInfo

        val androidInfo = DAndroidDeviceInfo()
        androidInfo.sdkVersion = Build.VERSION.SDK_INT
        androidInfo.versionCodeName = Build.VERSION.CODENAME
        androidInfo.securityPatch = Build.VERSION.SECURITY_PATCH
        androidInfo.bootloader = Build.BOOTLOADER
        androidInfo.fingerprint = Build.FINGERPRINT
        androidInfo.hardware = Build.HARDWARE
        androidInfo.radioVersion = Build.getRadioVersion() ?: ""
        androidInfo.board = Build.BOARD
        androidInfo.buildBrand = Build.BRAND
        androidInfo.buildHost = Build.HOST
        androidInfo.buildUser = Build.USER
        androidInfo.buildNumber = Build.DISPLAY
        androidInfo.product = Build.PRODUCT
        androidInfo.device = Build.DEVICE
        androidInfo.javaVmVersion = System.getProperty("java.vm.version") ?: ""
        androidInfo.glEsVersion = activityManager.deviceConfigurationInfo.glEsVersion
        androidInfo.serial = Build.SERIAL
        androidInfo.buildTime = Instant.fromEpochMilliseconds(Build.TIME)
        info.android = androidInfo

        return info
    }

    @SuppressLint("MissingPermission")
    fun getActiveSimCards(context: Context): List<SubscriptionInfo> {
        if (!Permission.READ_PHONE_STATE.can(context)) {
            return emptyList()
        }

        return subscriptionManager.activeSubscriptionInfoList ?: emptyList()
    }
}