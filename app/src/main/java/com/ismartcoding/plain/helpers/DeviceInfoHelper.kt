package com.ismartcoding.plain.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.activityManager
import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.subscriptionManager
import com.ismartcoding.plain.telephonyManager
import com.ismartcoding.plain.web.models.DPhoneNumber
import kotlinx.datetime.Instant


object DeviceInfoHelper {
    @SuppressLint("HardwareIds")
    fun getDeviceInfo(context: Context, readPhoneNumber: Boolean): DDeviceInfo {
        val deviceInfo = DDeviceInfo()
        deviceInfo.deviceName = PhoneHelper.getDeviceName(context)
        deviceInfo.releaseBuildVersion = Build.VERSION.RELEASE
        deviceInfo.versionCodeName = Build.VERSION.CODENAME
        deviceInfo.securityPatch = Build.VERSION.SECURITY_PATCH
        deviceInfo.bootloader = Build.BOOTLOADER
        deviceInfo.manufacturer = Build.MANUFACTURER
        deviceInfo.deviceId = ""
        deviceInfo.model = Build.MODEL
        deviceInfo.product = Build.PRODUCT
        deviceInfo.fingerprint = Build.FINGERPRINT
        deviceInfo.hardware = Build.HARDWARE
        deviceInfo.radioVersion = Build.getRadioVersion()
        deviceInfo.device = Build.DEVICE
        deviceInfo.board = Build.BOARD
        deviceInfo.displayVersion = Build.DISPLAY
        deviceInfo.buildBrand = Build.BRAND
        deviceInfo.buildHost = Build.HOST
        deviceInfo.buildTime = Instant.fromEpochMilliseconds(Build.TIME)
        deviceInfo.buildUser = Build.USER
        deviceInfo.serial = Build.SERIAL
        deviceInfo.language = java.util.Locale.getDefault().language
        deviceInfo.sdkVersion = Build.VERSION.SDK_INT
        deviceInfo.javaVmVersion = System.getProperty("java.vm.version")
        deviceInfo.kernelVersion = System.getProperty("os.version")
        deviceInfo.glEsVersion = activityManager.deviceConfigurationInfo.glEsVersion
        deviceInfo.screenDensity = android.util.DisplayMetrics.DENSITY_DEFAULT.toString()
        deviceInfo.screenHeight = android.util.DisplayMetrics().heightPixels
        deviceInfo.screenWidth = android.util.DisplayMetrics().widthPixels
        deviceInfo.uptime = SystemClock.elapsedRealtime()
        if (readPhoneNumber) {
            deviceInfo.phoneNumbers = getPhoneNumbers(context)
        }
        return deviceInfo
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    fun getPhoneNumbers(context: Context): List<DPhoneNumber> {
        if (Permission.READ_PHONE_STATE.can(context) && Permission.READ_PHONE_NUMBERS.can(context)) {
            val sims = mutableListOf<DPhoneNumber>()
            try {
                getActiveSimCards(context).forEach {
                    sims.add(DPhoneNumber(it.subscriptionId, it.displayName.toString(), it.number))
                }
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
            }
            return sims
        }

        return listOf()
    }

    @SuppressLint("MissingPermission")
    fun getActiveSimCards(context: Context): List<SubscriptionInfo> {
        if (!Permission.READ_PHONE_STATE.can(context)) {
            return emptyList()
        }

        return subscriptionManager.activeSubscriptionInfoList ?: emptyList()
    }
}