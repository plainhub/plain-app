package com.ismartcoding.plain

import java.util.regex.Pattern

object Constants {
    const val SSL_NAME = "Plain"
    const val DATABASE_NAME = "plain.db"
    const val GRAY_OUT_ALPHA = 0.4F
    const val NOTIFICATION_CHANNEL_ID = "default"
    const val DEFAULT_FOLDER_NAME = "SDCARD"
    const val CLICK_DRAG_TOLERANCE = 10f // Often, there will be a slight, unintentional, drag when the user taps the FAB, so we need to account for this.
    const val MAX_READABLE_TEXT_FILE_SIZE = 10 * 1024 * 1024 // 10 MB
    const val SUPPORT_EMAIL = "ismartcoding@gmail.com"
    const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"
    val EMAIL_PATTERN: Pattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}")
}
