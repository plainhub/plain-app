package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.AudioOverview
import com.ismartcoding.plain.models.MediaScanState
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.changes
import kotlinx.coroutines.flow.merge

object AudioOverviewClient : AudioOverviewApi()

open class AudioOverviewApi internal constructor(protected val api: GraphqlClient = GraphqlApi) {
    suspend fun read(): AudioOverview =
        api.execute(Query.audioOverview())
    val updates get() = merge(
        AppSubscriptions.contentChanged.changes(),
        AppSubscriptions.mediaLibraryUpdated.changes { DataType.AUDIO in it.types },
        AppSubscriptions.mediaScanProgress.changes {
            when (it.state) {
                MediaScanState.IDLE, MediaScanState.FAILED, MediaScanState.STOPPED, MediaScanState.PAUSED -> true
                MediaScanState.RUNNING -> false
            }
        },
    )
}
