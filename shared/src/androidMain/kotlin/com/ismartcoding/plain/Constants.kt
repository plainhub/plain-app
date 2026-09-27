package com.ismartcoding.plain

object AppIntents {
    val AUTHORITY: String get() = "${appContext.packageName}.provider"
    val ACTION_START_HTTP_SERVER: String get() = "${appContext.packageName}.action.START_HTTP_SERVER"
    val ACTION_STOP_HTTP_SERVER: String get() = "${appContext.packageName}.action.STOP_HTTP_SERVER"
    val ACTION_STOP_SCREEN_MIRROR: String get() = "${appContext.packageName}.action.STOP_SCREEN_MIRROR"
    val ACTION_PEER_CHAT_REPLY: String get() = "${appContext.packageName}.action.PEER_CHAT_REPLY"
    val ACTION_REPOST_HTTP_NOTIFICATION: String get() = "${appContext.packageName}.action.REPOST_HTTP_NOTIFICATION"
    val ACTION_PLAY_MEDIA: String get() = "${appContext.packageName}.action.PLAY_MEDIA"

    // Actions of the static launcher shortcuts. shortcuts.xml is generated per
    // variant (app/build.gradle.kts) with the variant applicationId as the
    // prefix, so deriving from the runtime package here always matches,
    // including the debug build's applicationIdSuffix.
    val ACTION_OPEN_NOTES: String get() = "${appContext.packageName}.action.OPEN_NOTES"
    val ACTION_OPEN_DOCS: String get() = "${appContext.packageName}.action.OPEN_DOCS"
    val ACTION_OPEN_POMODORO: String get() = "${appContext.packageName}.action.OPEN_POMODORO"
    val ACTION_OPEN_FEEDS: String get() = "${appContext.packageName}.action.OPEN_FEEDS"
    val ACTION_OPEN_IMAGES: String get() = "${appContext.packageName}.action.OPEN_IMAGES"
    val ACTION_OPEN_VIDEOS: String get() = "${appContext.packageName}.action.OPEN_VIDEOS"
    val ACTION_OPEN_AUDIO: String get() = "${appContext.packageName}.action.OPEN_AUDIO"
    val ACTION_OPEN_FILES: String get() = "${appContext.packageName}.action.OPEN_FILES"
}

object IntentExtras {
    const val CHAT_TARGET_ID = "chat_target_id"
    const val SHARE_IMAGE_PATH = "share_image_path"
    const val SHARE_IMAGE_NAME = "share_image_name"
}
