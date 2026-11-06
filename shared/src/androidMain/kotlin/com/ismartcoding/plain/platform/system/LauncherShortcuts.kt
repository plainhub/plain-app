package com.ismartcoding.plain.platform.system

import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ismartcoding.plain.app.AppIntents
import com.ismartcoding.plain.appContext

private val mainActivityClass: Class<*> by lazy {
    Class.forName("com.ismartcoding.plain.MainActivity")
}

internal actual suspend fun publishLauncherShortcuts(tools: List<AppFeatureType>) {
    val context = appContext
    val shortcuts = tools.map { type ->
        // Resource names (drawable shortcut_*, string shortcut_*) are the
        // lowercased AppFeatureType name — single naming source, no parallel ids.
        val resName = "shortcut_${type.name.lowercase()}"
        val intent = Intent(context, mainActivityClass).apply {
            action = AppIntents.actionOpenShortcut(type)
            `package` = context.packageName
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val labelRes = context.resources.getIdentifier(resName, "string", context.packageName)
        val label = if (labelRes != 0) context.getString(labelRes) else type.name
        ShortcutInfoCompat.Builder(context, resName)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, appResourceDrawable(resName)))
            .setIntent(intent)
            .build()
    }
    ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
}
