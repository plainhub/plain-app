package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.workers.FeedFetchWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual fun feedWorkerOneTimeRequest(feedId: String) {
    val request = OneTimeWorkRequestBuilder<FeedFetchWorker>()
        .setInputData(workDataOf("feed_id" to feedId))
        .addTag("feeds.one.sync")
        .build()
    WorkManager.getInstance(appContext).enqueue(request)
}

actual fun feedWorkerStartRepeat() {
    CoroutineScope(Dispatchers.IO).launch {
        FeedFetchWorker.startRepeatWorkerAsync(appContext)
    }
}

actual fun feedWorkerCancelRepeat() {
    FeedFetchWorker.cancelRepeatWorker()
}
