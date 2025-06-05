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
    const val LATEST_RELEASE_URL = "https://api.github.com/repos/ismartcoding/plain-app/releases/latest"
    const val ONE_DAY = 24 * 60 * 60L
    const val ONE_DAY_MS = ONE_DAY * 1000L
    const val BROADCAST_ACTION_SERVICE = "${BuildConfig.APPLICATION_ID}.action.service"
    const val BROADCAST_ACTION_ACTIVITY = "${BuildConfig.APPLICATION_ID}.action.activity"
    const val KEY_STORE_FILE_NAME = "keystore2.jks"
    const val MAX_MESSAGE_LENGTH = 2048 // Maximum length of a message in the chat
    const val TEXT_FILE_SUMMARY_LENGTH = 250
}
