package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.i18n.*

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.thumbnail.ThumbnailGenerator
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val mainActivityClass: Class<*> by lazy {
    Class.forName("com.ismartcoding.plain.MainActivity")
}

object MediaShortcutHelper {
    private const val ICON_SIZE = 256
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun addToDesktop(context: Context, path: String, label: String, iconRes: Int) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            DialogHelper.showMessage(Res.string.shortcut_not_supported)
            return
        }
        scope.launch {
            val bitmap = buildIconBitmap(context, path)
            val icon = bitmap?.let { IconCompat.createWithAdaptiveBitmap(it) }
                ?: IconCompat.createWithResource(context, iconRes)
            withContext(Dispatchers.Main) {
                pinShortcut(context, path, label, icon)
            }
        }
    }

    private fun pinShortcut(context: Context, path: String, label: String, icon: IconCompat) {
        val launchIntent = Intent(context, mainActivityClass).apply {
            action = AppIntents.ACTION_PLAY_MEDIA
            putExtra(Constants.EXTRA_MEDIA_PATH, path)
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val shortcutId = "media_${path.hashCode()}"
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(icon)
            .setIntent(launchIntent)
            .build()

        // Provide a PendingIntent callback so launchers (e.g. MIUI) that require it
        // can confirm the pin request. The broadcast is received but no extra action needed.
        val callbackIntent = Intent(context, mainActivityClass).apply {
            action = "${context.packageName}.action.SHORTCUT_PINNED"
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val successCallback = PendingIntent.getActivity(
            context,
            shortcutId.hashCode(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, successCallback.intentSender)
        // Show a toast so the user knows something happened, especially on launchers
        // that don't display a visible confirmation dialog (e.g. some MIUI versions).
        DialogHelper.showMessage(Res.string.shortcut_added_to_home)
    }

    // Thumbnail for image/video/SVG via the shared generator; album art for audio.
    // Falls back to null so the caller uses the app launcher icon.
    private suspend fun buildIconBitmap(context: Context, path: String): Bitmap? {
        var bitmap = runCatching {
            ThumbnailGenerator.getBitmapAsync(context, File(path), ICON_SIZE, ICON_SIZE, centerCrop = true)
        }.getOrNull()
        if (bitmap == null && path.isAudioFast()) {
            bitmap = runCatching { extractEmbeddedArtwork(path) }.getOrNull()
        }
        bitmap ?: return null
        val square = runCatching { ThumbnailUtils.extractThumbnail(bitmap, ICON_SIZE, ICON_SIZE) }
            .getOrNull() ?: bitmap
        // Shortcut icons are marshalled to the launcher; hardware bitmaps can't cross that boundary.
        return if (square.config == Bitmap.Config.HARDWARE) {
            runCatching { square.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull() ?: square
        } else {
            square
        }
    }

    private fun extractEmbeddedArtwork(path: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val art = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(art, 0, art.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= ICON_SIZE && bounds.outHeight / (sample * 2) >= ICON_SIZE) {
                sample *= 2
            }
            return BitmapFactory.decodeByteArray(
                art, 0, art.size,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )
        } finally {
            runCatching { retriever.release() }
        }
    }
}