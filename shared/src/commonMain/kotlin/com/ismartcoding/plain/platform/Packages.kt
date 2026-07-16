package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DCertificate
import com.ismartcoding.plain.features.file.FileSortBy
import kotlin.time.Instant

/**
 * CommonMain package descriptor with primitive fields only.
 *
 * Platform-specific code (Android `PackageInfo`/`ApplicationInfo`) is mapped
 * into this shape at the platform boundary so the rest of the app can stay
 * platform-agnostic.
 *
 * The optional detail fields ([sourceDir], [dataDir], [targetSdkVersion],
 * [minSdkVersion], [versionCode], [hasLargeHeap]) are populated only by
 * [getPackageDetail] — search results leave them at their defaults.
 */
data class DPackageInfo(
    val id: String,
    val name: String,
    val type: String,
    val version: String,
    val path: String,
    val size: Long,
    val installedAt: Instant,
    val updatedAt: Instant,
    val certs: List<DCertificate> = emptyList(),
    val sourceDir: String = "",
    val dataDir: String = "",
    val targetSdkVersion: Int = 0,
    val minSdkVersion: Int = 0,
    val versionCode: Long = 0L,
    val hasLargeHeap: Boolean = false,
)

/**
 * Result of [installPackage] — describes the post-install state of the package.
 *
 * @param packageName the package name parsed from the APK, or empty if unknown.
 * @param lastUpdateTime update time reported by the package manager after the
 *     install completed, or null if the package is brand-new (no row yet).
 * @param isNew true when the package was not previously installed.
 */
data class PackageInstallResult(
    val packageName: String,
    val lastUpdateTime: Instant?,
    val isNew: Boolean,
)

/**
 * Search installed packages matching [query], paginated by [limit]/[offset]
 * and sorted by [sortBy]. Returns an empty list on platforms without a
 * package manager.
 */
expect suspend fun searchPackages(
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<DPackageInfo>

/**
 * Count installed packages matching [query]. Returns 0 on unsupported platforms.
 */
expect suspend fun countPackages(query: String): Int

/**
 * Returns a map of package id -> DPackageInfo (or null if not installed) for
 * each id in [ids]. Missing ids map to null.
 */
expect suspend fun getPackageInfoMap(ids: List<String>): Map<String, DPackageInfo?>

/**
 * Uninstall the package identified by [id]. No-op on unsupported platforms.
 */
expect fun uninstallPackage(id: String)

/**
 * Install the APK at [path]. Returns a [PackageInstallResult] describing the
 * newly installed package, or throws on failure (e.g. file not found, parse
 * error, unsupported format).
 */
expect fun installPackage(path: String): PackageInstallResult

/**
 * Returns the launcher icon for the package identified by [packageId], suitable
 * for use as a Coil `AsyncImage` model. Returns null on platforms without a
 * package manager (iOS) or if the icon cannot be loaded.
 *
 * Android: returns the Android `Drawable` from `PackageManager.getApplicationIcon`.
 */
expect fun getApplicationIcon(packageId: String): Any?

/**
 * Returns full detail for the package identified by [id], or null if the package
 * is not installed or cannot be queried. Always null on iOS.
 */
expect suspend fun getPackageDetail(id: String): DPackageInfo?

/**
 * Whether the package identified by [id] is currently installed. Always false
 * on platforms without a package manager.
 */
expect fun isPackageInstalled(id: String): Boolean

/**
 * Whether the package identified by [id] has a launchable activity. Always
 * false on iOS.
 */
expect fun canLaunchPackage(id: String): Boolean

/**
 * Launch the package identified by [id]. No-op on iOS.
 */
expect fun launchPackage(id: String)

/**
 * Open the system "App info" settings screen for the package identified by
 * [id]. No-op on iOS.
 */
expect fun viewPackageInSettings(id: String)

/**
 * Parse the APK at [path] and return its AndroidManifest.xml as a formatted
 * XML string. Returns an empty string on iOS or on parse failure.
 */
expect fun getManifestXml(path: String): String
