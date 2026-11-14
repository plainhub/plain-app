package com.ismartcoding.plain.rust.host

import com.ismartcoding.plain.rust.native.RustHostReports
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.observeClipboardChanges
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal suspend fun observeClipboardReports() = observeClipboardWhenEnabled(
    Prefs.apiAccessFeatures.map { com.ismartcoding.plain.features.web.AccessFeature.CLIPBOARD in it },
) {
    observeClipboardChanges { report ->
        try {
            RustHostReports.clipboard(JsonHelper.jsonEncode(report))
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (error: Exception) {
            LogCat.e("Clipboard report", error)
        }
    }
}

internal suspend fun observeClipboardWhenEnabled(enabled: Flow<Boolean>, observe: suspend () -> Unit) {
    enabled.distinctUntilChanged().collectLatest { if (it) observe() }
}
