package com.ismartcoding.plain

object Constants {
    const val SSL_NAME = "Plain"
    const val DATABASE_NAME = "plain.db"
    const val NOTIFICATION_CHANNEL_ID = "default"
    const val CHAT_NOTIFICATION_CHANNEL_ID = "peer_chat"
    const val MAX_READABLE_TEXT_FILE_SIZE = 10 * 1024 * 1024 // 10 MB
    const val SUPPORT_EMAIL = "ismartcoding@gmail.com"
    const val LATEST_RELEASE_URL = "https://api.github.com/repos/ismartcoding/plain-app/releases/latest"
    const val ONE_DAY = 24 * 60 * 60L
    const val ONE_DAY_MS = ONE_DAY * 1000L
    const val KEY_STORE_FILE_NAME = "keystore.bks"
    const val MAX_MESSAGE_LENGTH = 2048 // Maximum length of a message in the chat
    const val TEXT_FILE_SUMMARY_LENGTH = 250
    const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"
    const val ACTION_STOP_HTTP_SERVER = "${BuildConfig.APPLICATION_ID}.action.STOP_HTTP_SERVER"
    const val ACTION_STOP_SCREEN_MIRROR = "${BuildConfig.APPLICATION_ID}.action.STOP_SCREEN_MIRROR"
    const val ACTION_PEER_CHAT_REPLY = "${BuildConfig.APPLICATION_ID}.action.PEER_CHAT_REPLY"
}
