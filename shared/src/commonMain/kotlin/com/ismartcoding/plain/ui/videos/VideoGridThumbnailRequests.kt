package com.ismartcoding.plain.ui.videos

import com.ismartcoding.plain.platform.system.ThumbnailNetworkSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class VideoGridThumbnailRequests(parallelism: Int = 6) {
    private val visible = MutableStateFlow(emptySet<String>())
    private val slots = Semaphore(parallelism)

    fun updateVisible(ids: List<String>, columns: Int = 1, forward: Boolean? = null) {
        val leadingItems = columns * 2
        visible.value = when (forward) {
            true -> ids.takeLast(leadingItems)
            false -> ids.take(leadingItems)
            null -> ids
        }.toSet()
    }

    fun forItem(id: String): ThumbnailNetworkSchedule = { load ->
        admit(id, load)
    }

    private suspend fun <T> admit(id: String, load: suspend () -> T): T {
        while (true) {
            visible.first { id in it }
            slots.withPermit {
                if (id in visible.value) return load()
            }
        }
    }
}
