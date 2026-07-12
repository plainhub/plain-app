package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.rss.model.RssChannel

expect suspend fun fetchRssChannel(url: String): RssChannel
