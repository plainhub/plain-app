package com.ismartcoding.plain.lib.mdns

/** Wall-clock milliseconds, used by the browser for follow-up query throttling. */
internal expect fun mdnsNowMillis(): Long
