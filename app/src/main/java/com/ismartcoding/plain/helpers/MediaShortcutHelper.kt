package com.ismartcoding.plain.helpers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.helpers.DialogHelper

object MediaShortcutHelper {
    fun addToDesktop(context: Context, path: String, label: String, iconRes: Int) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            DialogHelper.showMessage(R.string.shortcut_not_supported)
            return
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Constants.ACTION_PLAY_MEDIA
            putExtra(Constants.EXTRA_MEDIA_PATH, path)
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val shortcutId = "media_${path.hashCode()}"
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setIntent(launchIntent)
            .build()

        // Provide a PendingIntent callback so launchers (e.g. MIUI) that require it
        // can confirm the pin request. The broadcast is received but no extra action needed.
        val callbackIntent = Intent(context, MainActivity::class.java).apply {
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
        DialogHelper.showMessage(R.string.shortcut_added_to_home)
    }
}
