package com.ismartcoding.plain

object AppIntents {
    val AUTHORITY: String get() = "${appContext.packageName}.provider"
    val ACTION_START_HTTP_SERVER: String get() = "${appContext.packageName}.action.START_HTTP_SERVER"
    val ACTION_STOP_HTTP_SERVER: String get() = "${appContext.packageName}.action.STOP_HTTP_SERVER"
    val ACTION_STOP_SCREEN_MIRROR: String get() = "${appContext.packageName}.action.STOP_SCREEN_MIRROR"
    val ACTION_PEER_CHAT_REPLY: String get() = "${appContext.packageName}.action.PEER_CHAT_REPLY"
    val ACTION_REPOST_HTTP_NOTIFICATION: String get() = "${appContext.packageName}.action.REPOST_HTTP_NOTIFICATION"
    val ACTION_PLAY_MEDIA: String get() = "${appContext.packageName}.action.PLAY_MEDIA"
}
