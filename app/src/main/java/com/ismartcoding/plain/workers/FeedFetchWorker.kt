package com.ismartcoding.plain.workers

import android.content.Context
import androidx.work.*
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.isWifiConnected
import com.ismartcoding.lib.helpers.CoroutinesHelper.pmap
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.preference.FeedAutoRefreshIntervalPreference
import com.ismartcoding.plain.preference.FeedAutoRefreshOnlyWifiPreference
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.events.FeedStatusEvent
import com.ismartcoding.plain.features.feed.*
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.collections.chunked
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.set

class FeedFetchWorker(
    val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        supervisorScope {
            val autoRefresh = inputData.getBoolean("auto_refresh", false)
            if (autoRefresh && FeedAutoRefreshOnlyWifiPreference.getAsync(context) && context.isWifiConnected()) {
                return@supervisorScope
            }

            val feedId = inputData.getString("feed_id") ?: ""
            if (feedId.isNotEmpty()) {
                val feed = FeedHelper.getById(feedId) ?: return@supervisorScope
                if (statusMap[feed.id] == FeedWorkerStatus.PENDING) {
                    return@supervisorScope
                }
                syncFeedAsync(feed)
                val json = JSONObject()
                json.put("feedId", feedId)
                json.put("error", errorMap[feedId] ?: "")
                sendEvent(WebSocketEvent(EventType.FEEDS_FETCHED, json.toString()))
            } else {
                val feeds = FeedHelper.getAll()
                if (feeds.isEmpty()) {
                    return@supervisorScope
                }

                if (statusMap["all"] == FeedWorkerStatus.PENDING) {
                    return@supervisorScope
                }

                setStatusMap("all", FeedWorkerStatus.PENDING)
                feeds.chunked(16)
                    .forEach {
                        it.map { feed -> async { syncFeedAsync(feed) } }
                            .awaitAll()
                    }
                setStatusMap("all", FeedWorkerStatus.COMPLETED)
                val json = JSONObject()
                json.put("feedId", "all")
                json.put("error", errorMap.values.joinToString("\n"))
                sendEvent(WebSocketEvent(EventType.FEEDS_FETCHED, json.toString()))
            }
        }

        return Result.success()
    }

    private suspend fun syncFeedAsync(feed: DFeed) {
        setStatusMap(feed.id, FeedWorkerStatus.PENDING)
        try {
            LogCat.d("Syncing feed: ${feed.id}, ${feed.name}, ${feed.url}")
            val syndFeed = FeedHelper.fetchAsync(feed.url)
            val list = syndFeed.items.map { it.toDFeedEntry(feed.id, feed.url) }
            val inserted = FeedEntryHelper.feedEntryDao.insertListIfNotExist(list)
            if (feed.fetchContent) {
                inserted.chunked(4)
                    .forEach {
                        it.pmap { f ->
                            f.fetchContentAsync()
                        }
                    }
            }
            errorMap.remove(feed.id)
            setStatusMap(feed.id, FeedWorkerStatus.COMPLETED)
        } catch (ex: Throwable) {
            LogCat.e(ex)
            errorMap[feed.id] = ex.toString()
            setStatusMap(feed.id, FeedWorkerStatus.ERROR)
        }
    }

    companion object {
        private const val ONE_TIME_WORK_NAME = "feeds.one.sync"
        private const val REPEAT_WORK_NAME = "feeds.repeat.sync"

        val statusMap = mutableMapOf<String, FeedWorkerStatus>()
        val errorMap = mutableMapOf<String, String>()

        private fun setStatusMap(
            feedId: String,
            status: FeedWorkerStatus,
        ) {
            statusMap[feedId] = status
            sendEvent(FeedStatusEvent(feedId, status))
        }

        fun oneTimeRequest(feedId: String) {
            val data = Data.Builder()
            data.putString("feed_id", feedId)
            val request =
                OneTimeWorkRequestBuilder<FeedFetchWorker>()
                    .setInputData(data.build()).addTag(ONE_TIME_WORK_NAME)
            WorkManager.getInstance(MainApp.instance).enqueue(request.build())
        }

        fun cancelRepeatWorker() {
            WorkManager.getInstance(MainApp.instance).cancelAllWorkByTag(REPEAT_WORK_NAME)
            statusMap.clear()
            errorMap.clear()
        }

        suspend fun startRepeatWorkerAsync(context: Context) {
            val data = Data.Builder()
            data.putBoolean("auto_refresh", true)
            val request =
                PeriodicWorkRequestBuilder<FeedFetchWorker>(
                    FeedAutoRefreshIntervalPreference.getAsync(context).toLong(),
                    TimeUnit.SECONDS,
                ).setInputData(data.build()).setConstraints(
                    Constraints.Builder().build(),
                ).addTag(REPEAT_WORK_NAME)

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                REPEAT_WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request.build(),
            )
        }
    }
}
