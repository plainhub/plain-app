package com.ismartcoding.plain.rust

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
internal data class RustStartupConfig(
    val debug: Boolean,
    val cacheDir: String,
    val name: String,
    val deviceType: String,
    val buildChannel: String,
    val downloadsDir: String,
    val manufacturer: String = "",
    val model: String = "",
    val osName: String = "",
    val osVersion: String = "",
    val appVersion: String = "",
    val appBuildNumber: String = "",
    val cpuArch: String = "",
    val socModel: String? = null,
    val wifiAwareSupported: Boolean,
    val recentFilesCatalog: Boolean,
    val systemMediaCatalog: Boolean,
    val supportedAbis: List<String>,
    val updateSource: StartupUpdateSource? = null,
    val androidSdkVersion: Int? = null,
    val androidVersionCodeName: String? = null,
    val androidSecurityPatch: String? = null,
    val androidBootloader: String? = null,
    val androidFingerprint: String? = null,
    val androidHardware: String? = null,
    val androidRadioVersion: String? = null,
    val androidBoard: String? = null,
    val androidBuildBrand: String? = null,
    val androidBuildNumber: String? = null,
    val androidDevice: String? = null,
    val androidJavaVmVersion: String? = null,
    val androidGlEsVersion: String? = null,
    val androidBuildTime: Long? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val legacyStorage: LegacyStorage? = null,
)

@Serializable
internal data class LegacyStorage(
    val databaseDirectory: String,
    val preferencesDirectory: String,
    val filesDirectory: String,
)

@Serializable
internal data class StartupUpdateSource(
    val url: String,
    val assets: List<StartupUpdateAssetRule>,
    val pageUrl: String = "",
)

@Serializable
internal data class StartupUpdateAssetRule(
    val requiredAbis: List<String> = emptyList(),
    val nameContains: String = "",
    val nameSuffix: String = "",
)
