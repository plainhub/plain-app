package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.opml.OpmlParser
import com.ismartcoding.plain.lib.opml.OpmlWriter
import com.ismartcoding.plain.lib.opml.entity.Body
import com.ismartcoding.plain.lib.opml.entity.Head
import com.ismartcoding.plain.lib.opml.entity.Opml
import com.ismartcoding.plain.lib.opml.entity.Outline
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.helpers.TimeHelper

suspend fun FeedHelper.importAsync(content: String) = withIO {
    val feedList = mutableListOf<DFeed>()
    val opml = OpmlParser().parse(content)
    opml.body.outlines.forEach {
        if (it.subElements.isEmpty()) {
            if (it.attributes["xmlUrl"] != null) {
                feedList.add(
                    DFeed().apply {
                        name = it.getName()
                        url = it.getUrl()
                        fetchContent = it.attributes["fetchContent"] == "true"
                    },
                )
            }
        } else {
            it.subElements.forEach { outline ->
                feedList.add(
                    DFeed().apply {
                        name = outline.getName()
                        url = outline.getUrl()
                        fetchContent = outline.attributes["fetchContent"] == "true"
                    },
                )
            }
        }
    }

    val urls = getAll().map { it.url }
    val feedDao = AppDatabase.instance.feedDao()
    feedDao.insert(*feedList.distinctBy { it.url }.filter { !urls.contains(it.url) }.toTypedArray())
}

suspend fun FeedHelper.exportAsync(): String = withIO {
    val feeds = getAll()
    OpmlWriter().write(
        Opml(
            "2.0",
            Head(
                LocaleHelper.getString(Res.string.app_name),
                TimeHelper.now().toString(),
            ),
            Body(
                feeds.map { feed ->
                    Outline(
                        mapOf(
                            "text" to feed.name,
                            "title" to feed.name,
                            "xmlUrl" to feed.url,
                            "fetchContent" to feed.fetchContent.toString(),
                        ),
                        listOf(),
                    )
                },
            ),
        ),
    )
}
