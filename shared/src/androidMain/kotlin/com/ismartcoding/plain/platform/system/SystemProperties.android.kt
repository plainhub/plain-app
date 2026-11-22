package com.ismartcoding.plain.platform.system

import android.os.Build
import com.ismartcoding.plain.activityManager
import com.ismartcoding.plain.platform.network.wifiAwareAttached
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.platform.media.getDownloadsDirPath
import com.ismartcoding.plain.platform.network.isWifiAwareSupported
import com.ismartcoding.plain.rust.RustStartupConfig
import com.ismartcoding.plain.rust.LegacyStorage
import com.ismartcoding.plain.rust.StartupUpdateAssetRule
import com.ismartcoding.plain.rust.StartupUpdateSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale as JavaLocale

internal actual fun createRustStartupConfig(): RustStartupConfig {
    val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    return RustStartupConfig(
        debug = isDebugBuild(),
        cacheDir = cacheDirPath(),
        legacyStorage = LegacyStorage(
            databaseDirectory = appContext.getDatabasePath("plain.db").parentFile!!.absolutePath,
            preferencesDirectory = appContext.filesDir.absolutePath,
            filesDirectory = (appContext.getExternalFilesDir(null) ?: appContext.filesDir).absolutePath,
        ),
        name = PhoneHelper.getDeviceName(appContext),
        deviceType = getDeviceType().name,
        buildChannel = AppChannelType.fromString(buildChannel).name,
        downloadsDir = getDownloadsDirPath(),
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        osName = "Android",
        osVersion = Build.VERSION.RELEASE,
        appVersion = packageInfo.versionName ?: "",
        appBuildNumber = packageInfo.longVersionCode.toString(),
        cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "",
        socModel = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else null,
        wifiAwareSupported = isWifiAwareSupported,
        recentFilesCatalog = Build.VERSION.SDK_INT >= 29,
        systemMediaCatalog = true,
        supportedAbis = Build.SUPPORTED_ABIS.toList(),
        updateSource = if (buildChannel == AppChannelType.GITHUB.name) {
            StartupUpdateSource(
                url = "https://api.github.com/repos/plainhub/plain-app/releases/latest",
                pageUrl = "https://github.com/plainhub/plain-app/releases/latest",
                assets = listOf(
                    StartupUpdateAssetRule(
                        requiredAbis = listOf("arm64-v8a", "x86_64", "riscv64"),
                        nameContains = "Recommended",
                    ),
                    StartupUpdateAssetRule(nameSuffix = "-Old-32bit.apk"),
                ),
            )
        } else null,
        androidSdkVersion = Build.VERSION.SDK_INT,
        androidVersionCodeName = Build.VERSION.CODENAME,
        androidSecurityPatch = Build.VERSION.SECURITY_PATCH,
        androidBootloader = Build.BOOTLOADER,
        androidFingerprint = Build.FINGERPRINT,
        androidHardware = Build.HARDWARE,
        androidRadioVersion = Build.getRadioVersion() ?: "",
        androidBoard = Build.BOARD,
        androidBuildBrand = Build.BRAND,
        androidBuildNumber = Build.DISPLAY,
        androidDevice = Build.DEVICE,
        androidJavaVmVersion = System.getProperty("java.vm.version") ?: "",
        androidGlEsVersion = activityManager.deviceConfigurationInfo.glEsVersion,
        androidBuildTime = Build.TIME,
    )
}

internal actual fun readSystemProperties(names: List<String>): Map<String, JsonElement> {
    val display by lazy {
        val metrics = appContext.resources.displayMetrics
        Triple(metrics.widthPixels, metrics.heightPixels, metrics.density)
    }
    return names.associateWith { name ->
        when (name) {
            "wifiAwareRunning" -> JsonPrimitive(wifiAwareAttached.value)
            "language" -> JsonPrimitive(JavaLocale.getDefault().language)
            "display.width" -> JsonPrimitive(display.first)
            "display.height" -> JsonPrimitive(display.second)
            "display.density" -> JsonPrimitive(display.third)
            else -> error("Unsupported system property: $name")
        }
    }
}
