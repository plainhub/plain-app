package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.i18n.*

import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.lib.opml.OpmlParser
import com.ismartcoding.plain.lib.opml.OpmlWriter
import com.ismartcoding.plain.lib.opml.entity.Body
import com.ismartcoding.plain.lib.opml.entity.Head
import com.ismartcoding.plain.lib.opml.entity.Opml
import com.ismartcoding.plain.lib.opml.entity.Outline
import com.ismartcoding.plain.lib.rss.RssParser
import com.ismartcoding.plain.lib.rss.model.RssChannel
import com.ismartcoding.plain.api.KtorClientFactory
import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedCount
import com.ismartcoding.plain.db.FeedDao
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.workers.FeedFetchWorker
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import java.io.Reader
import java.io.Writer
import java.util.Date

object FeedHelper {
    private val feedDao: FeedDao by lazy {
        AppDatabase.instance.feedDao()
    }

    suspend fun getAll(): List<DFeed> = withIO {
        feedDao.getAll()
    }

    suspend fun getFeedCounts(): List<DFeedCount> = withIO {
        feedDao.getFeedCounts()
    }

    suspend fun getById(id: String): DFeed? = withIO {
        feedDao.getById(id)
    }

    suspend fun getByUrl(url: String): DFeed? = withIO {
        feedDao.getByUrl(url)
    }

    suspend fun addAsync(updateItem: DFeed.() -> Unit): String = withIO {
        val item = DFeed()
        updateItem(item)
        feedDao.insert(item)
        item.id
    }

    suspend fun updateAsync(
        id: String,
        updateItem: DFeed.() -> Unit,
    ): String = withIO {
        val item = feedDao.getById(id) ?: return@withIO id
        item.updatedAt = TimeHelper.now()
        updateItem(item)
        feedDao.update(item)
        id
    }

    suspend fun deleteAsync(ids: Set<String>) = withIO {
        ids.forEach {
            FeedFetchWorker.errorMap.remove(it)
            FeedFetchWorker.statusMap.remove(it)
        }
        feedDao.delete(ids)
    }

    suspend fun importAsync(reader: Reader) = withIO {
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

        val urls = feedDao.getAll().map { it.url }
        feedDao.insert(*feedList.distinctBy { it.url }.filter { !urls.contains(it.url) }.toTypedArray())
    }

    suspend fun exportAsync(writer: Writer) = withIO {
        val feeds = feedDao.getAll()
        val result =
            OpmlWriter().write(
                Opml(
                    "2.0",
                    Head(
                        LocaleHelper.getString(Res.string.app_name),
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

    suspend fun fetchAsync(url: String): RssChannel = withIO {
        val r = KtorClientFactory.httpClient().get(url)
        if (r.status != HttpStatusCode.OK) {
            throw Exception("HTTP ${r.status.value} ${r.status.description}")
        }
        val xmlString = r.bodyAsText()
        val rssParser = RssParser()
        rssParser.parse(xmlString)
    }
}
