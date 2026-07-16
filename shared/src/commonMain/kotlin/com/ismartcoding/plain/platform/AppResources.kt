package com.ismartcoding.plain.platform

/**
 * Platform resource identifier lookup. On Android this resolves resource names
 * (e.g. "ic_launcher") to integer resource ids via `Resources.getIdentifier`.
 * On iOS all functions return `0` since there is no equivalent resource id system.
 */
expect object AppResources {
    fun color(name: String): Int
    fun drawable(name: String): Int
    fun mipmap(name: String): Int
}
