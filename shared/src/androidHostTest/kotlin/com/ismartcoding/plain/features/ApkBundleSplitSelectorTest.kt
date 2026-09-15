package com.ismartcoding.plain.features

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApkBundleSplitSelectorTest {
    private fun device(
        abis: List<String> = listOf("arm64-v8a", "armeabi-v7a"),
        dpi: Int = 480,
        locales: List<String> = listOf("zh-CN", "en-US"),
    ) = ApkBundleDevice(abis, dpi, locales)

    private fun select(vararg names: String, device: ApkBundleDevice = device()) =
        ApkBundleSplitSelector.select(names.toList(), device).toSet()

    @Test
    fun saiLayoutPicksDeviceConfigs() {
        val selected = select(
            "split_config.armeabi_v7a.apk",
            "split_config.arm64_v8a.apk",
            "split_config.xhdpi.apk",
            "split_config.xxhdpi.apk",
            "split_config.en.apk",
        )
        assertEquals(setOf("split_config.arm64_v8a.apk", "split_config.xxhdpi.apk", "split_config.en.apk"), selected)
    }

    @Test
    fun bundletoolLayoutPicksDeviceConfigs() {
        val selected = select(
            "splits/base-arm64_v8a.apk",
            "splits/base-x86.apk",
            "splits/base-hdpi.apk",
            "splits/base-xxhdpi.apk",
            "splits/base-en.apk",
            "splits/base-zh.apk",
        )
        assertEquals(setOf("splits/base-arm64_v8a.apk", "splits/base-xxhdpi.apk", "splits/base-zh.apk"), selected)
    }

    @Test
    fun featureModuleKeepsMasterAndDeviceConfig() {
        val selected = select(
            "split_myfeature.apk",
            "split_myfeature.config.arm64_v8a.apk",
            "split_myfeature.config.x86.apk",
        )
        assertEquals(setOf("split_myfeature.apk", "split_myfeature.config.arm64_v8a.apk"), selected)
    }

    @Test
    fun noMatchingAbiIsExcluded() {
        val selected = select("split_config.x86.apk", "split_config.x86_64.apk")
        assertTrue(selected.isEmpty())
    }

    @Test
    fun abiUnderscoreAndHyphenNormalize() {
        val selected = select("split_config.arm64-v8a.apk", device = device(abis = listOf("arm64-v8a")))
        assertEquals(setOf("split_config.arm64-v8a.apk"), selected)
    }

    @Test
    fun languageFallsBackToExactThenPrefixThenEnglish() {
        assertEquals(
            setOf("split_config.pt-BR.apk"),
            select("split_config.pt.apk", "split_config.pt-BR.apk", device = device(locales = listOf("pt-BR"))),
        )
        assertEquals(
            setOf("split_config.zh.apk"),
            select("split_config.zh.apk", "split_config.en.apk", device = device(locales = listOf("zh-Hans-CN"))),
        )
        assertEquals(
            setOf("split_config.en.apk"),
            select("split_config.fr.apk", "split_config.en.apk", device = device(locales = listOf("de-DE"))),
        )
        assertTrue(select("split_config.fr.apk", device = device(locales = listOf("de-DE"))).isEmpty())
    }

    @Test
    fun densityPicksNearestBucket() {
        assertEquals(
            setOf("split_config.xhdpi.apk"),
            select("split_config.mdpi.apk", "split_config.hdpi.apk", "split_config.xhdpi.apk", device = device(dpi = 320)),
        )
        assertEquals(
            setOf("split_config.hdpi.apk"),
            select("split_config.mdpi.apk", "split_config.hdpi.apk", "split_config.xhdpi.apk", device = device(dpi = 213)),
        )
    }

    @Test
    fun densityTiePrefersRealDensityOverNodpi() {
        assertEquals(
            setOf("split_config.xhdpi.apk"),
            select("split_config.nodpi.apk", "split_config.xhdpi.apk", device = device(dpi = 320)),
        )
    }

    @Test
    fun featureMasterFromBundletoolNamingIsKept() {
        val selected = select("myfeature-master.apk", "myfeature-arm64_v8a.apk", "myfeature-en.apk")
        assertEquals(setOf("myfeature-master.apk", "myfeature-arm64_v8a.apk", "myfeature-en.apk"), selected)
    }

    @Test
    fun sidecarEntries() {
        assertTrue(BundleApkInstaller.isSidecar("standalones/base-arm64_v8a.apk"))
        assertTrue(BundleApkInstaller.isSidecar("instant/base-master.apk"))
        assertFalse(BundleApkInstaller.isSidecar("splits/base-master.apk"))
        assertFalse(BundleApkInstaller.isSidecar("base.apk"))
    }
}
