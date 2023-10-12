package com.ismartcoding.plain.ui.base.subsampling.helpers

import android.os.Looper

internal object BackgroundUtils {

  internal val isMainThread: Boolean
    get() = Thread.currentThread() === Looper.getMainLooper().thread

  internal fun ensureMainThread() {
    if (isMainThread) {
      return
    }

    error("Cannot be executed on a background thread!")
  }

  internal fun ensureBackgroundThread() {
    if (!isMainThread) {
      return
    }

    error("Cannot be executed on the main thread!")
  }
}
