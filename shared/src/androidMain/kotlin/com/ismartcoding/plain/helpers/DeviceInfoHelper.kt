package com.ismartcoding.plain.helpers

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.platform.parseCpuInfoModel
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.getAppVersionName
import com.ismartcoding.plain.getAppVersionCode
import com.ismartcoding.plain.activityManager
import com.ismartcoding.plain.data.DAndroidExtras
import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.data.DDisplayInfo
import com.ismartcoding.plain.data.DevicePlatform
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
        info.appVersion = getAppVersionName()
        info.appBuildNumber = getAppVersionCode().toString()
        info.language = java.util.Locale.getDefault().language
        info.cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        info.cpuModel = cpuModel()

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

        val androidInfo = DAndroidExtras()
        androidInfo.sdkVersion = Build.VERSION.SDK_INT
        androidInfo.versionCodeName = Build.VERSION.CODENAME
        androidInfo.securityPatch = Build.VERSION.SECURITY_PATCH
        androidInfo.bootloader = Build.BOOTLOADER
        androidInfo.fingerprint = Build.FINGERPRINT
        androidInfo.hardware = Build.HARDWARE
        androidInfo.radioVersion = Build.getRadioVersion() ?: ""
        androidInfo.board = Build.BOARD
        androidInfo.buildBrand = Build.BRAND
        androidInfo.buildNumber = Build.DISPLAY
        androidInfo.device = Build.DEVICE
        androidInfo.javaVmVersion = System.getProperty("java.vm.version") ?: ""
        androidInfo.glEsVersion = activityManager.deviceConfigurationInfo.glEsVersion
        androidInfo.buildTime = Instant.fromEpochMilliseconds(Build.TIME)
        info.android = androidInfo

        return info
    }

    private fun cpuModel(): String {
        if (Build.VERSION.SDK_INT >= 31) {
            val soc = Build.SOC_MODEL
            if (soc.isNotBlank() && soc != "unknown") return soc
        }
        // ARM kernels usually omit "model name" in /proc/cpuinfo; x86 images have it.
        val content = runCatching { java.io.File("/proc/cpuinfo").readText() }.getOrNull() ?: return ""
        return parseCpuInfoModel(content)
    }

    @SuppressLint("MissingPermission")
    fun getActiveSimCards(context: Context): List<SubscriptionInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        return subscriptionManager.activeSubscriptionInfoList ?: emptyList()
    }
}