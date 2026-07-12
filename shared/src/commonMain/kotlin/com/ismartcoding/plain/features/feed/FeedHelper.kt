package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedCount
import com.ismartcoding.plain.db.FeedDao
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.platform.feedWorkerOneTimeRequest

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
        ids.forEach { FeedWorkerState.clear(it) }
        feedDao.delete(ids)
    }

    fun fetchOneTime(feedId: String) {
        feedWorkerOneTimeRequest(feedId)
    }
}
