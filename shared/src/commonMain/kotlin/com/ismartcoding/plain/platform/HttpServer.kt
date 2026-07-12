package com.ismartcoding.plain.platform

/**
 * Set of HTTP/HTTPS ports that failed to bind on the current platform.
 * Empty when no port conflicts exist.
 */
expect fun httpServerPortsInUse(): Set<Int>
