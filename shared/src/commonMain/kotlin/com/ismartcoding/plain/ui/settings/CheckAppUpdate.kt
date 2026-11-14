package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.features.settings.UpdatesClient
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.check_failure
import com.ismartcoding.plain.i18n.rate_limit
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.CancellationException

suspend fun checkAppUpdate(showToast: Boolean, automatic: Boolean = false): Boolean? = try {
    val status = UpdatesClient.check(automatic)
    when (status) {
        "AVAILABLE" -> true
        "UP_TO_DATE" -> false
        "NOT_DUE" -> null
        "RATE_LIMITED" -> {
            if (showToast) DialogHelper.showMessage(Res.string.rate_limit)
            null
        }
        else -> error("Unknown update status: $status")
    }
} catch (canceled: CancellationException) {
    throw canceled
} catch (error: Exception) {
    if (showToast) DialogHelper.showMessage(Res.string.check_failure)
    null
}
