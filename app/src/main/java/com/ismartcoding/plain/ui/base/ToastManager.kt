package com.ismartcoding.plain.ui.base

import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.lib.channel.sendEvent

data class ToastEvent(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val duration: Long = 2000L
) : ChannelEvent()

object ToastManager {
    fun showToast(message: String, type: ToastType = ToastType.INFO, duration: Long = 2000L) {
        sendEvent(ToastEvent(message, type, duration))
    }

    fun showInfoToast(message: String, duration: Long = 2000L) {
        showToast(message, ToastType.INFO, duration)
    }

    fun showSuccessToast(message: String, duration: Long = 2000L) {
        showToast(message, ToastType.SUCCESS, duration)
    }

    fun showWarningToast(message: String, duration: Long = 2000L) {
        showToast(message, ToastType.WARNING, duration)
    }

    fun showErrorToast(message: String, duration: Long = 2000L) {
        showToast(message, ToastType.ERROR, duration)
    }
}

