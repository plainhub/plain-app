package com.ismartcoding.plain.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.copied
import com.ismartcoding.plain.platform.LocaleHelper

/**
 * Invisible activity that briefly takes window focus so the clipboard can be
 * read on Android 10+ while the app is in the background. Same workaround as
 * KDE Connect: a background read attempt gets denied by the system (and logged
 * by ClipboardService); ClipboardWatcher's logcat thread spots that log line
 * and launches this activity, which reads the clipboard on focus and finishes.
 *
 * Also the target of the quick-settings tile tap, where [showToast] confirms
 * the capture with a toast (KDE Connect's "send clipboard" feedback).
 *
 * Requires "Display over other apps" (SYSTEM_ALERT_WINDOW) for reliable
 * background launches.
 */
class ClipboardFloatingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this))
        window.apply {
            attributes = attributes.apply {
                dimAmount = 0f
                flags = flags or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            ClipboardWatcher.handleFocusGained()
            if (intent.getBooleanExtra(KEY_SHOW_TOAST, false)) {
                Toast.makeText(this, LocaleHelper.getString(Res.string.copied), Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    companion object {
        private const val KEY_SHOW_TOAST = "SHOW_TOAST"

        fun getIntent(context: Context): Intent =
            Intent(context, ClipboardFloatingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        fun getIntentWithToast(context: Context): Intent =
            getIntent(context).putExtra(KEY_SHOW_TOAST, true)
    }
}
