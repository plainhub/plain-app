@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.lib.extensions

import platform.Foundation.NSURLComponents

internal actual fun String.hasUrlHost(): Boolean = !NSURLComponents.componentsWithString(this)?.host.isNullOrEmpty()
