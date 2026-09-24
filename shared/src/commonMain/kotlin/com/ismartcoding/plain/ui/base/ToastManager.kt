package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.lib.ChannelEvent
import com.ismartcoding.plain.lib.sendEvent

data class ToastEvent(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 2000L,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
) : ChannelEvent()

object ToastManager {
    fun showToast(
        message: String,
        type: ToastType = ToastType.INFO,
        durationMs: Long = 2000L,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        sendEvent(ToastEvent(message, type, durationMs, actionLabel, onAction))
    }

    fun showInfoToast(message: String, durationMs: Long = 2000L) {
        showToast(message, ToastType.INFO, durationMs)
    }

    fun showSuccessToast(message: String, durationMs: Long = 2000L) {
        showToast(message, ToastType.SUCCESS, durationMs)
    }

    fun showWarningToast(message: String, durationMs: Long = 2000L) {
        showToast(message, ToastType.WARNING, durationMs)
    }

    fun showErrorToast(message: String, durationMs: Long = 2000L) {
        showToast(message, ToastType.ERROR, durationMs)
    }
}

