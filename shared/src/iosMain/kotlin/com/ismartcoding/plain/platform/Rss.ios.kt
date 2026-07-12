package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.rss.model.RssChannel

actual suspend fun fetchRssChannel(url: String): RssChannel {
    throw NotImplementedError("RSS fetching not yet implemented on iOS")
}
