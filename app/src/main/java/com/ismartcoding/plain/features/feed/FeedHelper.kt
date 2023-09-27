package com.ismartcoding.plain.features.feed

import com.ismartcoding.lib.opml.OpmlParser
import com.ismartcoding.lib.opml.OpmlWriter
import com.ismartcoding.lib.opml.entity.Body
import com.ismartcoding.lib.opml.entity.Head
import com.ismartcoding.lib.opml.entity.Opml
import com.ismartcoding.lib.opml.entity.Outline
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.FeedDao
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.workers.FeedFetchWorker
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.datetime.Clock
import java.io.Reader
import java.io.Writer
import java.util.*

object FeedHelper {
    val feedDao: FeedDao by lazy {
        AppDatabase.instance.feedDao()
    }

    fun getAll(): List<DFeed> {
        return feedDao.getAll()
    }

    fun getById(id: String): DFeed? {
        return feedDao.getById(id)
    }

    fun getByUrl(url: String): DFeed? {
        return feedDao.getByUrl(url)
    }

    fun addAsync(updateItem: DFeed.() -> Unit): String {
        val item = DFeed()
        updateItem(item)
        feedDao.insert(item)
        return item.id
    }

    fun updateAsync(
        id: String,
        updateItem: DFeed.() -> Unit,
    ): String {
        val item = feedDao.getById(id) ?: return id
        item.updatedAt = Clock.System.now()
        updateItem(item)
        feedDao.update(item)
        return id
    }

    fun deleteAsync(ids: Set<String>) {
        ids.forEach {
            FeedFetchWorker.errorMap.remove(it)
            FeedFetchWorker.statusMap.remove(it)
        }
        feedDao.delete(ids)
    }

    fun import(reader: Reader) {
        val feedList = mutableListOf<DFeed>()
        val opml = OpmlParser().parse(reader)
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
        feedDao.insert(*feedList.filter { !urls.contains(it.url) }.toTypedArray())
    }

    suspend fun export(writer: Writer) {
        val feeds = getAll()
        val result =
            OpmlWriter().write(
                Opml(
                    "2.0",
                    Head(
                        LocaleHelper.getString(R.string.app_name),
                        Date().toString(),
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
        writer.write(result)
        writer.close()
    }

    suspend fun fetchAsync(url: String): SyndFeed {
        val r = HttpClientManager.httpClient().get(url)
        val input = r.bodyAsChannel().toInputStream()
        return SyndFeedInput().build(XmlReader(input))
    }
}
