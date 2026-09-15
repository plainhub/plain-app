package com.ismartcoding.plain.features

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.StatFs
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.apk.ApkParsers
import com.ismartcoding.plain.lib.apk.bean.ApkMeta
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.PackageInstallResult
import java.io.File
import java.util.zip.ZipFile

/** Device profile passed into the pure split selector so it stays host-testable. */
internal data class ApkBundleDevice(val abiOrder: List<String>, val densityDpi: Int, val localeTags: List<String>)

/**
 * Installs zip-based APK bundles (.apkm / .apks / .xapk): unpacks every *.apk
 * entry, picks the base APK plus the device-matching splits and streams them
 * into a single PackageInstaller session.
 */
object BundleApkInstaller {
    internal const val ACTION_INSTALL_STATUS = "com.ismartcoding.plain.action.PACKAGE_INSTALL_STATUS"

    val BUNDLE_EXTENSIONS = setOf("apkm", "apks", "xapk")

    fun isBundle(file: File): Boolean = file.extension.lowercase() in BUNDLE_EXTENSIONS

    fun install(bundle: File): PackageInstallResult {
        val cacheDir = appContext.cacheDir
        if (StatFs(cacheDir.path).availableBytes < bundle.length() * 2) {
            throw IllegalArgumentException("Insufficient storage space to unpack ${bundle.name}")
        }
        val tmpDir = File(cacheDir, "apk-bundle-" + System.currentTimeMillis())
        try {
            return installApks(bundle.name, extractApks(bundle, tmpDir))
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    private fun extractApks(bundle: File, tmpDir: File): List<ExtractedApk> {
        ZipFile(bundle).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                .toList()
            if (entries.isEmpty()) {
                throw IllegalArgumentException("No APK found inside ${bundle.name}")
            }
            val prefix = tmpDir.canonicalPath + File.separator
            return entries.map { entry ->
                val out = File(tmpDir, entry.name)
                if (!out.canonicalPath.startsWith(prefix)) {
                    throw SecurityException("Illegal zip entry: ${entry.name}")
                }
                out.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input -> out.outputStream().use { input.copyTo(it) } }
                ExtractedApk(entry.name, out, runCatching { ApkParsers.getMetaInfo(out) }.getOrNull())
            }
        }
    }

    // bundletool .apks stores alternatives (standalone apex/instant builds) outside splits/
    internal fun isSidecar(entryName: String): Boolean = entryName.substringBefore('/') in setOf("standalones", "instant", "apex")

    private fun installApks(bundleName: String, apks: List<ExtractedApk>): PackageInstallResult {
        val mains = apks.filter { !isSidecar(it.entryName) }
        val base = mains
            .filter { it.meta?.split == null }
            .minByOrNull {
                when {
                    it.file.name == "base.apk" -> 0
                    it.file.name.endsWith("-master.apk") -> 1
                    else -> 2
                }
            }
            ?: return installStandalone(bundleName, apks)

        val packageName = base.meta?.packageName
        if (packageName.isNullOrEmpty()) {
            throw IllegalArgumentException("Failed to parse package name from base APK")
        }
        val isNew = !PackageHelper.isInstalled(packageName)
        val device = ApkBundleDevice(
            abiOrder = Build.SUPPORTED_ABIS.toList(),
            densityDpi = appContext.resources.configuration.densityDpi,
            localeTags = appContext.resources.configuration.locales.toLanguageTags().split(',').map { it.trim() },
        )
        val selected = ApkBundleSplitSelector
            .select(mains.filter { it !== base }.map { it.entryName }, device)
            .toSet()
        val files = listOf(base.file) + mains.filter { it !== base && it.entryName in selected }.map { it.file }
        LogCat.d("Installing bundle $bundleName: $packageName + ${files.size - 1} split(s)")
        startSession(files)
        return PackageInstallResult(packageName, null, isNew)
    }

    private fun installStandalone(bundleName: String, apks: List<ExtractedApk>): PackageInstallResult {
        val standalone = apks.asSequence()
            .filter { it.meta != null }
            .sortedBy { it.entryName }
            .firstOrNull { apk -> Build.SUPPORTED_ABIS.any { apk.entryName.contains(it, ignoreCase = true) } }
            ?: apks.firstOrNull { it.meta != null }
            ?: throw IllegalArgumentException("No installable APK found in $bundleName")
        LogCat.d("Installing standalone APK from $bundleName: ${standalone.entryName}")
        val packageName = standalone.meta?.packageName ?: ""
        val isNew = !PackageHelper.isInstalled(packageName)
        PackageHelper.install(appContext, standalone.file)
        return PackageInstallResult(packageName, null, isNew)
    }

    private fun startSession(apks: List<File>) {
        val installer = appContext.packageManager.packageInstaller
        val sessionId = installer.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))
        val session = installer.openSession(sessionId)
        try {
            apks.forEachIndexed { index, apk ->
                val out = session.openWrite("apk_$index.apk", 0, apk.length())
                apk.inputStream().use { input -> input.copyTo(out) }
                session.fsync(out)
                out.close()
            }
            val callback = Intent(ACTION_INSTALL_STATUS).setPackage(appContext.packageName)
            val pending = PendingIntent.getBroadcast(
                appContext,
                sessionId,
                callback,
                // the system writes EXTRA_STATUS/EXTRA_INTENT into this intent before sending it
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            session.commit(pending.intentSender)
        } catch (e: Exception) {
            runCatching { session.abandon() }
            throw e
        } finally {
            session.close()
        }
    }

    internal data class ExtractedApk(val entryName: String, val file: File, val meta: ApkMeta?)
}

/**
 * Pure split selection: parses standard AAB-derived split file names
 * (split_config.<cfg>.apk / <module>-<cfg>.apk) and, per module, keeps the
 * masters plus exactly one config per group (ABI / density / language / other).
 */
internal object ApkBundleSplitSelector {
    internal data class Split(val entryName: String, val module: String, val config: String?)

    private val ABIS = setOf("arm64_v8a", "armeabi_v7a", "armeabi", "x86_64", "x86", "mips", "mips64")
    private val DENSITIES = mapOf(
        "ldpi" to 120,
        "mdpi" to 160,
        "tvdpi" to 213,
        "hdpi" to 240,
        "xhdpi" to 320,
        "xxhdpi" to 480,
        "xxxhdpi" to 640,
    )
    private val LANG = Regex("^[a-z]{2,3}([_-][0-9]{3}|[_-]r?[a-z]{2})?$", RegexOption.IGNORE_CASE)

    internal enum class Kind { ABI, DENSITY, LANG, OTHER }

    internal fun parse(entryName: String): Split {
        val name = entryName.substringAfterLast('/').removeSuffix(".apk")
        val module: String
        val config: String?
        when {
            name == "base" -> {
                module = "base"
                config = null
            }
            name.startsWith("split_config.") -> {
                module = "base"
                config = name.removePrefix("split_config.")
            }
            name.startsWith("split_") -> {
                val rest = name.removePrefix("split_")
                val idx = rest.indexOf(".config.")
                if (idx > 0) {
                    module = rest.take(idx)
                    config = rest.substring(idx + ".config.".length)
                } else {
                    module = rest
                    config = null
                }
            }
            name.endsWith("-master") -> {
                module = name.removeSuffix("-master")
                config = null
            }
            name.contains('-') -> {
                module = name.substringBefore('-')
                config = name.substringAfter('-')
            }
            else -> {
                module = name
                config = null
            }
        }
        return Split(entryName, module, config)
    }

    internal fun kindOf(config: String): Kind = when {
        config.replace('-', '_') in ABIS -> Kind.ABI
        config == "nodpi" || config in DENSITIES -> Kind.DENSITY
        LANG.matches(config) -> Kind.LANG
        else -> Kind.OTHER
    }

    internal fun select(splitEntryNames: List<String>, device: ApkBundleDevice): List<String> {
        val splits = splitEntryNames.map { parse(it) }
        val selected = mutableSetOf<String>()
        for (module in splits.map { it.module }.distinct()) {
            val own = splits.filter { it.module == module }
            own.filter { it.config == null }.forEach { selected += it.entryName }
            for (kind in Kind.entries) {
                val candidates = own.filter { val c = it.config; c != null && kindOf(c) == kind }
                bestOf(candidates, kind, device)?.let { selected += it.entryName }
            }
        }
        return selected.toList()
    }

    private fun bestOf(candidates: List<Split>, kind: Kind, device: ApkBundleDevice): Split? {
        if (candidates.isEmpty()) return null
        return when (kind) {
            Kind.ABI -> {
                val byAbi = candidates.associateBy { it.config!!.replace('-', '_') }
                device.abiOrder.asSequence()
                    .map { it.replace('-', '_') }
                    .mapNotNull { byAbi[it] }
                    .firstOrNull()
            }
            Kind.DENSITY -> candidates.minWithOrNull(
                compareBy(
                    { split -> if (split.config == "nodpi") 0 else kotlin.math.abs((DENSITIES[split.config] ?: 0) - device.densityDpi) },
                    // on a tie prefer a real density match over nodpi
                    { it.config == "nodpi" },
                ),
            )
            Kind.LANG -> {
                val byTag = candidates.associateBy { normalizeTag(it.config!!) }
                device.localeTags.asSequence()
                    .map(::normalizeTag)
                    .mapNotNull { byTag[it] }
                    .firstOrNull()
                    ?: device.localeTags.asSequence()
                        .map { normalizeTag(it).substringBefore('-') }
                        .mapNotNull { lang -> candidates.firstOrNull { normalizeTag(it.config!!).substringBefore('-') == lang } }
                        .firstOrNull()
                    ?: byTag["en"]
            }
            Kind.OTHER -> candidates.first()
        }
    }

    private fun normalizeTag(tag: String): String =
        tag.lowercase().replace('_', '-').replace(Regex("-r([a-z]{2})$"), "-$1")
}
