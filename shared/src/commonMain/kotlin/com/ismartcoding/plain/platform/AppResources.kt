package com.ismartcoding.plain.platform

/**
 * Platform resource identifier lookup. On Android this resolves resource names
 * (e.g. "ic_launcher") to integer resource ids via `Resources.getIdentifier`.
 * On iOS all functions return `0` since there is no equivalent resource id system.
 */

/** Resolve a color resource name to its integer resource id (0 if not found). */
expect fun appResourceColor(name: String): Int

/** Resolve a drawable resource name to its integer resource id (0 if not found). */
expect fun appResourceDrawable(name: String): Int

/** Resolve a mipmap resource name to its integer resource id (0 if not found). */
expect fun appResourceMipmap(name: String): Int
