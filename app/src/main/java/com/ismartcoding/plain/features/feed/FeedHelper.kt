package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.FeedDao
import com.ismartcoding.plain.workers.FeedFetchWorker
import com.rometools.opml.feed.opml.Attribute
import com.rometools.opml.feed.opml.Opml
import com.rometools.opml.feed.opml.Outline
import com.rometools.opml.io.impl.OPML20Generator
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.WireFeedInput
import com.rometools.rome.io.WireFeedOutput
import com.rometools.rome.io.XmlReader
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.datetime.Clock
import java.io.Reader
import java.io.Writer
import java.net.URL
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

    fun updateAsync(id: String, updateItem: DFeed.() -> Unit): String {
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
        val opml = WireFeedInput().build(reader) as Opml
        opml.outlines.forEach { outline ->
            if (!outline.xmlUrl.isNullOrEmpty()) {
                feedList.add(DFeed().apply {
                    name = outline.title
                    url = outline.xmlUrl
                    fetchContent = outline.getAttributeValue("fetchContent") == "true"
                })
            }

            outline.children.forEach {
                if (!outline.xmlUrl.isNullOrEmpty()) {
                    feedList.add(DFeed().apply {
                        name = it.title
                        url = it.xmlUrl
                        fetchContent = it.getAttributeValue("fetchContent") == "true"
                    })
                }
            }
        }

        val urls = getAll().map { it.url }
        feedDao.insert(*feedList.filter { !urls.contains(it.url) }.toTypedArray())
    }

     suspend fun export(writer: Writer) {
        val feeds = getAll()
        val opml = Opml().apply {
            feedType = OPML20Generator().type
            encoding = "utf-8"
            created = Date()
            outlines = feeds.map {
                Outline(it.name, URL(it.url), null).apply {
                    if (it.fetchContent) {
                        attributes.add(Attribute("fetchContent", "true"))
                    }
                }
            }
        }
        WireFeedOutput().output(opml, writer)
    }

    suspend fun fetchAsync(url: String): SyndFeed {
        val r = HttpClientManager.httpClient().get(url)
        val input = r.bodyAsChannel().toInputStream()
        return SyndFeedInput().build(XmlReader(input))
    }
}