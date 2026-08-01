package com.ismartcoding.plain.workers

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.feed.FeedFetcher
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.preferences.FeedAutoRefreshIntervalPreference
import java.util.concurrent.TimeUnit

class FeedFetchWorker(
    val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val autoRefresh = inputData.getBoolean("auto_refresh", false)
        val feedId = inputData.getString("feed_id") ?: ""
        if (feedId.isNotEmpty()) {
            FeedFetcher.fetchOne(feedId)
        } else {
            FeedFetcher.fetchAll(autoRefresh)
        }
        return Result.success()
    }

    companion object {
        private const val ONE_TIME_WORK_NAME = "feeds.one.sync"
        private const val REPEAT_WORK_NAME = "feeds.repeat.sync"

        fun oneTimeRequest(feedId: String) {
            val request = OneTimeWorkRequestBuilder<FeedFetchWorker>()
                .setInputData(workDataOf("feed_id" to feedId))
                .addTag(ONE_TIME_WORK_NAME)
                .build()
            WorkManager.getInstance(appContext).enqueue(request)
        }

        fun cancelRepeatWorker() {
            WorkManager.getInstance(appContext).cancelAllWorkByTag(REPEAT_WORK_NAME)
            com.ismartcoding.plain.features.feed.FeedWorkerState.clearAll()
        }

        suspend fun startRepeatWorkerAsync(context: Context) = withIO {
            val request =
                PeriodicWorkRequestBuilder<FeedFetchWorker>(
                    FeedAutoRefreshIntervalPreference.getAsync().toLong(),
                    TimeUnit.SECONDS,
                ).setInputData(workDataOf("auto_refresh" to true))
                    .setConstraints(androidx.work.Constraints.Builder().build())
                    .addTag(REPEAT_WORK_NAME)

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                REPEAT_WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request.build(),
            )
        }
    }
}
