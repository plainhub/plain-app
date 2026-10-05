package com.ismartcoding.plain.ui.page.sharedfolder

import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.features.share.SharedBrowseResult
import com.ismartcoding.plain.features.share.SharedLinkClient
import kotlinx.coroutines.CancellationException

internal suspend fun fetchSharedInfoWithFallback(messageId: String, msg: DMessageShare, virtualPath: String?): SharedBrowseResult? =
    try { SharedLinkClient.browse(messageId, msg, virtualPath) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }
